package com.veleda.cyclewise.session

import android.util.Log
import androidx.sqlite.db.SupportSQLiteDatabase
import com.veleda.cyclewise.androidData.local.database.PeriodDatabase
import com.veleda.cyclewise.androidData.local.database.RekeyVerificationFailedException
import com.veleda.cyclewise.androidData.local.draft.LockedWaterDraft
import com.veleda.cyclewise.di.SESSION_SCOPE
import com.veleda.cyclewise.domain.repository.PeriodRepository
import com.veleda.cyclewise.domain.services.PassphraseService
import com.veleda.cyclewise.settings.AppSettings
import com.veleda.cyclewise.ui.auth.WaterDraftSyncer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.parameter.parametersOf

/**
 * Centralizes all Koin session scope lifecycle operations.
 *
 * This is the **only** class in the application that implements [KoinComponent] for
 * programmatic scope access. ViewModels receive it via normal constructor injection,
 * keeping them decoupled from the DI framework.
 *
 * ## Responsibilities
 * - **[openSession]** — Closes any stale scope, creates a fresh session scope, derives
 *   the encryption key, force-opens the database, prepopulates on first unlock, and
 *   syncs water drafts.
 * - **[closeSession]** — Closes the active session scope (destroying the database and
 *   all session-scoped dependencies). Does **not** emit a [SessionBus] logout signal;
 *   callers handle post-close signaling.
 * - **[isSessionActive]** — Returns `true` when a session scope exists.
 * - **[changePassphrase]** — Verifies the current passphrase via the session-scoped
 *   [KeyFingerprintHolder], rekeys the database, and returns a sealed result.
 *
 * @param appSettings      Used to check/set the `isPrepopulated` flag on first unlock.
 * @param lockedWaterDraft  Used to sync water drafts after a successful unlock.
 */
class SessionManager(
    private val appSettings: AppSettings,
    private val lockedWaterDraft: LockedWaterDraft,
) : KoinComponent {

    /**
     * Opens a new encrypted session for the given [passphrase].
     *
     * ## Steps
     * 1. Close any stale session scope so Koin does not return a cached database.
     * 2. Create a fresh Koin session scope and resolve the [PeriodDatabase] via
     *    [parametersOf] (triggers key derivation and `copyOf()` handoff).
     * 3. Force-open the database (`db.openHelper.writableDatabase`) so SQLCipher
     *    consumes the real key. An incorrect passphrase throws here.
     * 4. On first-ever unlock, prepopulate the symptom library.
     * 5. Sync water-intake drafts from [LockedWaterDraft].
     *
     * Runs entirely on [Dispatchers.IO].
     *
     * @param passphrase the raw user-entered passphrase string.
     * @throws Exception if the passphrase is wrong or database creation fails.
     */
    suspend fun openSession(passphrase: String) {
        val needsPrepopulation = !appSettings.isPrepopulated.first()

        withContext(Dispatchers.IO) {
            val koin = getKoin()

            // Always close a stale session scope so the PeriodDatabase is
            // re-created with the NEW passphrase. Without this, Koin returns
            // the cached (already-open) database from the old scope, bypassing
            // passphrase validation entirely.
            koin.getScopeOrNull("session")?.close()

            val sessionScope = koin.createScope(
                scopeId = "session",
                qualifier = SESSION_SCOPE,
            )

            val db = sessionScope.get<PeriodDatabase> { parametersOf(passphrase) }
            db.openHelper.writableDatabase   // Force-open so SQLCipher consumes the real key

            val repository = sessionScope.get<PeriodRepository>()

            if (needsPrepopulation) {
                repository.prepopulateSymptomLibrary()
                appSettings.setPrepopulated(true)
            }

            // Medication prepopulation runs unconditionally — INSERT IGNORE makes it
            // idempotent, and this ensures existing users who already passed the
            // first-unlock gate still get the medication library populated.
            repository.prepopulateMedicationLibrary()
            val syncer = WaterDraftSyncer(lockedWaterDraft, repository)
            syncer.sync()
        }
    }

    /**
     * Closes the active session scope, destroying the database and all session-scoped
     * dependencies.
     *
     * Does **not** emit a [SessionBus] logout signal — callers handle post-close
     * signaling (e.g., navigation to the passphrase screen).
     *
     * Safe to call when no session is active (no-op).
     */
    fun closeSession() {
        getKoin().getScopeOrNull("session")?.close()
    }

    /**
     * Returns `true` when a Koin session scope exists (i.e., the database is unlocked).
     */
    val isSessionActive: Boolean
        get() = getKoin().getScopeOrNull("session") != null

    /**
     * Returns the open [SupportSQLiteDatabase] from the active session, or `null` if
     * no session is active.
     *
     * Used by the backup export flow to checkpoint the WAL before copying the database
     * file. Since only [SessionManager] is a [KoinComponent], this method provides a
     * clean way for singleton-scoped ViewModels to access the session database without
     * becoming Koin-aware themselves.
     */
    fun getOpenDatabase(): SupportSQLiteDatabase? {
        return getKoin().getScopeOrNull("session")
            ?.get<PeriodDatabase>()
            ?.openHelper
            ?.writableDatabase
    }

    /**
     * Changes the database encryption passphrase.
     *
     * ## Steps
     * 1. Derives the current key and verifies it against the session-scoped
     *    [KeyFingerprintHolder] (SHA-256 fingerprint comparison).
     * 2. Derives the new key and calls [PeriodDatabase.changeEncryptionKey]
     *    (`PRAGMA rekey`) on the active session database.
     * 3. Updates the fingerprint holder with the new key.
     *
     * Runs entirely on [Dispatchers.IO]. Returns a sealed [ChangePassphraseResult]
     * instead of throwing.
     *
     * @param current       the user's current passphrase.
     * @param newPassphrase the desired new passphrase.
     * @return [ChangePassphraseResult] indicating success or the type of failure.
     */
    suspend fun changePassphrase(
        current: String,
        newPassphrase: String,
    ): ChangePassphraseResult = withContext(Dispatchers.IO) {
        val passphraseService = getKoin().get<PassphraseService>()
        val sessionScope = getKoin().getScopeOrNull("session")
            ?: return@withContext ChangePassphraseResult.Failed

        val fingerprintHolder = sessionScope.get<KeyFingerprintHolder>()
        val currentKey = passphraseService.deriveKey(current)
        var newKey: ByteArray? = null

        try {
            if (!fingerprintHolder.matches(currentKey)) {
                return@withContext ChangePassphraseResult.WrongCurrent
            }

            newKey = passphraseService.deriveKey(newPassphrase)
            val context = getKoin().get<android.content.Context>()
            val db = sessionScope.get<PeriodDatabase>()
            // changeEncryptionKey zeroizes both key copies in its finally block
            db.changeEncryptionKey(context, currentKey.copyOf(), newKey.copyOf())

            // Rekey verified — update fingerprint with the new key
            fingerprintHolder.store(newKey)
            ChangePassphraseResult.Success
        } catch (e: RekeyVerificationFailedException) {
            Log.e("SessionManager", "Change passphrase verification failed: ${e.message}")
            ChangePassphraseResult.VerificationFailed
        } catch (e: Exception) {
            Log.e("SessionManager", "Change passphrase failed: ${e.message}")
            ChangePassphraseResult.Failed
        } finally {
            currentKey.fill(0)
            newKey?.fill(0)
        }
    }
}

/**
 * Result of a [SessionManager.changePassphrase] operation.
 *
 * Using a sealed interface instead of exceptions keeps the ViewModel's event handling
 * simple and exhaustive via `when`.
 */
sealed interface ChangePassphraseResult {
    /** The passphrase was changed and the fingerprint holder updated. */
    data object Success : ChangePassphraseResult

    /** The current passphrase did not match the stored fingerprint. */
    data object WrongCurrent : ChangePassphraseResult

    /** The post-rekey verification query failed (with or without rollback). */
    data object VerificationFailed : ChangePassphraseResult

    /** An unexpected error occurred (no active session, IO failure, etc.). */
    data object Failed : ChangePassphraseResult
}
