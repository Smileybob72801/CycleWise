package com.veleda.cyclewise.ui.auth

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.veleda.cyclewise.domain.models.BackupMetadata
import com.veleda.cyclewise.services.BackupException
import com.veleda.cyclewise.services.BackupManager
import com.veleda.cyclewise.session.SessionManager
import com.veleda.cyclewise.settings.AppSettings
import com.veleda.cyclewise.ui.backup.ImportStep
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Minimum passphrase length enforced during first-time setup. */
internal const val MIN_PASSPHRASE_LENGTH = 8

/** Maximum passphrase length accepted by the input fields. */
internal const val MAX_PASSPHRASE_LENGTH = 256

/**
 * UI state for the passphrase screen.
 *
 * @property isUnlocking        `true` while an unlock attempt is in progress; used by the UI
 *                              to show a loading indicator and by [reduce] as a re-entrancy
 *                              guard to ignore duplicate tap events.
 * @property isFirstTime        `true` when the user has never completed setup (derived from
 *                              `AppSettings.isPrepopulated`). When `true` the UI shows the
 *                              onboarding setup flow instead of the unlock screen.
 * @property isFirstTimeLoaded  `true` once [isFirstTime] has been resolved from DataStore.
 *                              The UI should not render until this is `true` to avoid a
 *                              flash of the wrong screen.
 * @property passphraseError    Inline error for the passphrase field during setup (e.g. too
 *                              short). `null` means no error.
 * @property confirmationError  Inline error for the confirmation field during setup (e.g.
 *                              mismatch). `null` means no error.
 * @property importStep             Current step in the backup import flow.
 * @property importMetadata         Parsed metadata from the selected backup archive.
 * @property importUri              SAF URI of the selected backup file.
 * @property importPassphraseError  Inline error for the backup passphrase dialog.
 * @property importConfirmText      Current text in the "type OVERWRITE" confirmation field.
 * @property importError            Error message from the import flow, or `null`.
 * @property isVerifyingPassphrase  Whether passphrase verification is in progress.
 * @property importPassphrase       The verified passphrase for the import (stored transiently).
 */
data class PassphraseUiState(
    val isUnlocking: Boolean = false,
    val isFirstTime: Boolean = false,
    val isFirstTimeLoaded: Boolean = false,
    val passphraseError: String? = null,
    val confirmationError: String? = null,
    val importStep: ImportStep = ImportStep.IDLE,
    val importMetadata: BackupMetadata? = null,
    val importUri: Uri? = null,
    val importPassphraseError: String? = null,
    val importConfirmText: String = "",
    val importError: String? = null,
    val isVerifyingPassphrase: Boolean = false,
    val importPassphrase: String? = null,
)

/**
 * Handles passphrase unlock and session scope lifecycle.
 *
 * On [PassphraseEvent.UnlockClicked], delegates to [SessionManager.openSession] which:
 * 1. Closes any stale session scope to force full passphrase re-validation.
 * 2. Creates a fresh Koin session scope and resolves the encrypted database.
 * 3. Force-opens the database so SQLCipher consumes the real key.
 * 4. Prepopulates the symptom library on first unlock.
 * 5. Syncs water drafts from [LockedWaterDraft] into the database.
 * 6. Emits [PassphraseEffect.NavigateToTracker] on success.
 *
 * Uses a pure [reduce] function for synchronous state transitions (validation,
 * re-entrancy guard). Side effects (session operations) are launched in [onEvent]
 * only when [reduce] transitions [PassphraseUiState.isUnlocking] from false to true.
 *
 * **Error handling:** On failure, the session scope is closed and [PassphraseEffect.ShowError] is emitted.
 *
 * Singleton-scoped (survives screen rotation).
 *
 * @param appSettings    Used to determine first-time vs returning user.
 * @param sessionManager Centralizes all session scope lifecycle operations.
 * @param backupManager  Handles `.rwbackup` archive validation, passphrase verification,
 *                       and import for the backup import flow.
 */
class PassphraseViewModel(
    private val appSettings: AppSettings,
    private val sessionManager: SessionManager,
    private val backupManager: BackupManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PassphraseUiState())

    /** Observable UI state consumed by the passphrase Compose screen. */
    val uiState: StateFlow<PassphraseUiState> = _uiState.asStateFlow()

    private val _effect = MutableSharedFlow<PassphraseEffect>(replay = 0)

    /**
     * One-shot side-effects consumed by the UI (navigation, error toasts).
     *
     * Uses `replay = 0` so effects are not replayed on recomposition or resubscription.
     */
    val effect: SharedFlow<PassphraseEffect> = _effect.asSharedFlow()

    init {
        viewModelScope.launch {
            val prepopulated = appSettings.isPrepopulated.first()
            _uiState.update {
                it.copy(
                    isFirstTime = !prepopulated,
                    isFirstTimeLoaded = true,
                )
            }
        }
    }

    /**
     * Single entry-point for all screen-level user interactions.
     *
     * Applies a pure state update via [reduce], then launches the unlock side effect
     * only when the state transitions to [PassphraseUiState.isUnlocking].
     */
    fun onEvent(event: PassphraseEvent) {
        val wasUnlocking = _uiState.value.isUnlocking
        _uiState.update { reduce(it, event) }

        // Launch the unlock side effect only when reduce transitioned isUnlocking false→true
        if (!wasUnlocking && _uiState.value.isUnlocking) {
            val passphrase = when (event) {
                is PassphraseEvent.UnlockClicked -> event.passphrase
                is PassphraseEvent.SetupClicked -> event.passphrase
                else -> return
            }
            launchUnlock(passphrase)
        }

        // Import side effects
        when (event) {
            is PassphraseEvent.ImportBackupClicked -> {
                viewModelScope.launch { _effect.emit(PassphraseEffect.LaunchImportPicker) }
            }
            is PassphraseEvent.ImportFileSelected -> launchValidateBackup(event.uri)
            is PassphraseEvent.ImportPassphraseEntered -> launchVerifyImportPassphrase(event.passphrase)
            is PassphraseEvent.ImportSecondConfirmed -> launchImportFromPassphraseScreen()
            else -> { /* state-only or handled above */ }
        }
    }

    /**
     * Pure function that returns the new [PassphraseUiState] for a given event.
     *
     * Contains no side effects — session operations are handled in [onEvent]
     * after the state has been updated.
     *
     * Handles:
     * - **UnlockClicked**: re-entrancy guard — if already unlocking, returns state unchanged.
     *   Otherwise sets `isUnlocking = true`.
     * - **SetupClicked**: validates passphrase length and confirmation match. Sets error
     *   fields on failure, or `isUnlocking = true` on success.
     */
    @Suppress("CyclomaticComplexMethod")
    private fun reduce(state: PassphraseUiState, event: PassphraseEvent): PassphraseUiState {
        return when (event) {
            is PassphraseEvent.UnlockClicked -> {
                if (state.isUnlocking) state
                else state.copy(isUnlocking = true)
            }
            is PassphraseEvent.SetupClicked -> {
                val cleared = state.copy(passphraseError = null, confirmationError = null)
                when {
                    event.passphrase.length < MIN_PASSPHRASE_LENGTH ->
                        cleared.copy(passphraseError = "too_short")
                    event.passphrase != event.confirmation ->
                        cleared.copy(confirmationError = "mismatch")
                    cleared.isUnlocking -> cleared
                    else -> cleared.copy(isUnlocking = true)
                }
            }

            // ── Backup Import ───────────────────────────────────────
            is PassphraseEvent.ImportBackupClicked -> state // effect-only
            is PassphraseEvent.ImportFileSelected -> state // side effect validates
            is PassphraseEvent.ImportMetadataConfirmed ->
                state.copy(importStep = ImportStep.PASSPHRASE_ENTRY, importPassphraseError = null)
            is PassphraseEvent.ImportPassphraseEntered ->
                state.copy(isVerifyingPassphrase = true, importPassphraseError = null)
            is PassphraseEvent.ImportFirstWarningConfirmed ->
                state.copy(importStep = ImportStep.SECOND_CONFIRM, importConfirmText = "")
            is PassphraseEvent.ImportConfirmTextChanged ->
                state.copy(importConfirmText = event.text)
            is PassphraseEvent.ImportSecondConfirmed ->
                state.copy(importStep = ImportStep.IMPORTING, importConfirmText = "")
            is PassphraseEvent.ImportDismissed -> state.copy(
                importStep = ImportStep.IDLE,
                importMetadata = null,
                importUri = null,
                importPassphraseError = null,
                importConfirmText = "",
                importError = null,
                isVerifyingPassphrase = false,
                importPassphrase = null,
            )
        }
    }

    /**
     * Orchestrates passphrase validation and session scope creation via [SessionManager].
     *
     * Delegates the entire scope lifecycle to [SessionManager.openSession], which handles
     * stale scope cleanup, scope creation, key derivation, database force-open,
     * prepopulation, and water draft sync.
     *
     * ## Error handling
     * On any exception the session scope is closed (destroying the database and
     * zeroizing SQLCipher's key copy) and [PassphraseEffect.ShowError] is emitted.
     *
     * @param passphrase the raw user-entered passphrase string.
     */
    private fun launchUnlock(passphrase: String) {
        val isSetup = _uiState.value.isFirstTime
        viewModelScope.launch {
            try {
                sessionManager.openSession(passphrase)
                if (isSetup) {
                    _effect.emit(PassphraseEffect.SetupComplete)
                } else {
                    _effect.emit(PassphraseEffect.NavigateToTracker)
                }
            } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                Log.e("PassphraseUnlock", "Unlock failed: ${e.message}")
                sessionManager.closeSession()
                _effect.emit(PassphraseEffect.ShowError("Failed to unlock. Wrong passphrase?"))
            } finally {
                _uiState.update { it.copy(isUnlocking = false) }
            }
        }
    }

    // ── Backup Import side effects ──────────────────────────────────

    /**
     * Validates the selected backup file and shows the metadata preview dialog.
     */
    private fun launchValidateBackup(uri: Uri) {
        viewModelScope.launch {
            try {
                val metadata = backupManager.validateBackup(uri)
                _uiState.update {
                    it.copy(
                        importStep = ImportStep.METADATA_PREVIEW,
                        importMetadata = metadata,
                        importUri = uri,
                        importError = null,
                    )
                }
            } catch (e: BackupException.SchemaVersionTooNew) {
                _uiState.update {
                    it.copy(importError = "schema_too_new:${e.backup}:${e.current}")
                }
            } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                Log.e("PassphraseVM", "Backup validation failed", e)
                _uiState.update { it.copy(importError = e.message) }
            }
        }
    }

    /**
     * Verifies the passphrase against the imported backup's database.
     *
     * On success:
     * - **First-time users (setup screen):** Skips double confirmation and immediately
     *   imports, then opens a session with the verified passphrase.
     * - **Returning users (unlock screen):** Advances to the first overwrite warning.
     */
    private fun launchVerifyImportPassphrase(passphrase: String) {
        viewModelScope.launch {
            val uri = _uiState.value.importUri ?: return@launch
            try {
                backupManager.verifyPassphrase(uri, passphrase)

                if (_uiState.value.isFirstTime) {
                    // Fresh install — no existing data, skip confirmation, import immediately
                    _uiState.update {
                        it.copy(
                            importStep = ImportStep.IMPORTING,
                            isVerifyingPassphrase = false,
                            importPassphrase = passphrase,
                        )
                    }
                    performImportAndOpenSession(uri, passphrase)
                } else {
                    // Returning user — show overwrite warning
                    _uiState.update {
                        it.copy(
                            importStep = ImportStep.FIRST_WARNING,
                            isVerifyingPassphrase = false,
                            importPassphraseError = null,
                            importPassphrase = passphrase,
                        )
                    }
                }
            } catch (e: BackupException.WrongPassphrase) {
                _uiState.update {
                    it.copy(
                        isVerifyingPassphrase = false,
                        importPassphraseError = "wrong_passphrase",
                    )
                }
            } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                Log.e("PassphraseVM", "Passphrase verification failed", e)
                _uiState.update {
                    it.copy(
                        isVerifyingPassphrase = false,
                        importPassphraseError = e.message,
                    )
                }
            }
        }
    }

    /**
     * Executes import from the passphrase screen (unlock screen path after double confirm).
     */
    private fun launchImportFromPassphraseScreen() {
        viewModelScope.launch {
            val uri = _uiState.value.importUri ?: return@launch
            val passphrase = _uiState.value.importPassphrase ?: return@launch
            performImportAndOpenSession(uri, passphrase)
        }
    }

    /**
     * Replaces the database and salt, then opens a session with the verified passphrase.
     *
     * Used by both the setup screen (directly after passphrase verification) and the
     * unlock screen (after double confirmation).
     */
    private suspend fun performImportAndOpenSession(uri: Uri, passphrase: String) {
        try {
            withContext(Dispatchers.IO) {
                sessionManager.closeSession()
                backupManager.importBackup(uri)
            }
            // Mark as prepopulated so the setup screen is skipped next time
            appSettings.setPrepopulated(true)
            // Open session with the imported backup's passphrase
            sessionManager.openSession(passphrase)
            _uiState.update {
                it.copy(
                    importStep = ImportStep.IDLE,
                    importMetadata = null,
                    importUri = null,
                    importPassphrase = null,
                )
            }
            _effect.emit(PassphraseEffect.NavigateToTracker)
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            Log.e("PassphraseVM", "Import failed", e)
            sessionManager.closeSession()
            _uiState.update {
                it.copy(
                    importStep = ImportStep.IDLE,
                    importError = e.message,
                    importPassphrase = null,
                )
            }
        }
    }
}
