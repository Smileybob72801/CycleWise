package com.veleda.cyclewise.di

import com.veleda.cyclewise.androidData.local.draft.LockedWaterDraft
import android.content.Context
import android.util.Log
import com.veleda.cyclewise.domain.repository.PeriodRepository
import com.veleda.cyclewise.services.SaltStorage
import com.veleda.cyclewise.ui.auth.WaterTrackerViewModel
import com.veleda.cyclewise.ui.tracker.TrackerViewModel
import org.koin.core.module.dsl.*
import org.koin.dsl.module
import org.koin.android.ext.koin.androidContext
import com.veleda.cyclewise.domain.services.PassphraseService
import com.veleda.cyclewise.services.PassphraseServiceAndroid
import com.veleda.cyclewise.androidData.local.database.PeriodDatabase
import com.veleda.cyclewise.androidData.local.database.rekeyRaw
import com.veleda.cyclewise.androidData.repository.RoomPeriodRepository
import com.veleda.cyclewise.domain.insights.InsightEngine
import com.veleda.cyclewise.domain.insights.generators.CycleLengthAverageGenerator
import com.veleda.cyclewise.domain.insights.generators.CycleLengthTrendGenerator
import com.veleda.cyclewise.domain.insights.generators.MoodPhasePatternGenerator
import com.veleda.cyclewise.domain.insights.generators.NextPeriodPredictionGenerator
import com.veleda.cyclewise.domain.insights.generators.SymptomPhasePatternGenerator
import com.veleda.cyclewise.domain.insights.generators.SymptomRecurrenceGenerator
import com.veleda.cyclewise.domain.insights.generators.EnergyPhasePatternGenerator
import com.veleda.cyclewise.domain.insights.generators.LibidoPhasePatternGenerator
import com.veleda.cyclewise.domain.insights.generators.FlowPatternGenerator
import com.veleda.cyclewise.domain.insights.generators.WaterIntakePhasePatternGenerator
import com.veleda.cyclewise.domain.insights.generators.SymptomSeverityTrendGenerator
import com.veleda.cyclewise.domain.insights.generators.CycleSummaryGenerator
import com.veleda.cyclewise.domain.insights.generators.CrossVariableCorrelationGenerator
import com.veleda.cyclewise.domain.insights.analysis.CorrelationEngine
import com.veleda.cyclewise.domain.insights.DataReadinessCalculator
import com.veleda.cyclewise.domain.insights.InsightScorer
import com.veleda.cyclewise.domain.insights.charts.ChartDataGenerator
import com.veleda.cyclewise.androidData.local.EducationalContentLoader
import com.veleda.cyclewise.androidData.local.providers.StaticEducationalContentProvider
import com.veleda.cyclewise.domain.providers.EducationalContentProvider
import com.veleda.cyclewise.domain.providers.MedicationLibraryProvider
import com.veleda.cyclewise.domain.providers.SymptomLibraryProvider
import com.veleda.cyclewise.domain.usecases.AutoCloseOngoingPeriodUseCase
import com.veleda.cyclewise.domain.usecases.DebugSeederUseCase
import com.veleda.cyclewise.domain.usecases.DeleteAllDataUseCase
import com.veleda.cyclewise.domain.usecases.TutorialCleanupUseCase
import com.veleda.cyclewise.domain.usecases.TutorialSeederUseCase
import org.koin.core.qualifier.named
import com.veleda.cyclewise.domain.usecases.GetOrCreateDailyLogUseCase
import com.veleda.cyclewise.session.KeyFingerprintHolder
import com.veleda.cyclewise.session.SessionBus
import com.veleda.cyclewise.session.SessionManager
import com.veleda.cyclewise.ui.coachmark.HintPreferences
import com.veleda.cyclewise.ui.log.DailyLogViewModel
import com.veleda.cyclewise.settings.AppSettings
import com.veleda.cyclewise.ui.auth.PassphraseViewModel
import com.veleda.cyclewise.reminders.ReminderScheduler
import com.veleda.cyclewise.ui.insights.InsightsViewModel
import com.veleda.cyclewise.ui.settings.SettingsViewModel
import kotlinx.datetime.LocalDate
import org.koin.core.qualifier.Qualifier

/**
 * Koin qualifier for the session-scoped DI lifetime.
 *
 * The session scope is created after a successful passphrase unlock and destroyed on
 * logout or autolock. It contains the encrypted database, all DAOs, the repository,
 * use cases, library providers, and session-bound ViewModels.
 */
val SESSION_SCOPE: Qualifier = named("UnlockedSessionScope")

/**
 * Transparently re-encrypts a legacy zero-key database with the correct passphrase-derived key.
 *
 * ## Background
 *
 * Before the `key.copyOf()` fix (commit `0a9406c`), [createDatabaseAndZeroizeKey] passed the
 * Argon2-derived key **by reference** to [SupportFactory], then immediately zeroized it. Because
 * [SupportFactory] stores the reference (not a copy), SQLCipher read an all-zeros array when the
 * database was actually opened. This means **all databases created before the fix are encrypted
 * with a 32-byte zero key, regardless of the user's passphrase.**
 *
 * This function detects and fixes that situation by:
 * 1. Checking whether the database file exists (no-op if it doesn't).
 * 2. Attempting to open the file with a 32-byte zero key via raw SQLCipher.
 * 3. If it opens: executing `PRAGMA rekey` to re-encrypt with the real derived key.
 * 4. If the zero-key open fails: the database is already properly encrypted — skip silently.
 *
 * After migration the zero-key open will fail on subsequent unlocks, so the overhead is a single
 * failed SQLCipher open (~50 ms) per unlock — negligible compared to the 1–3 s Argon2 derivation.
 *
 * @param context    application context for resolving the database file path.
 * @param correctKey a **copy** of the real passphrase-derived key (will be zeroized by this
 *                   method in its `finally` block).
 */
internal fun migrateLegacyZeroKeyIfNeeded(context: Context, correctKey: ByteArray) {
    val dbFile = context.getDatabasePath("cyclewise.db")
    if (!dbFile.exists()) {
        correctKey.fill(0)
        return
    }

    val zeroKey = ByteArray(32)
    try {
        net.sqlcipher.database.SQLiteDatabase.loadLibs(context)
        val db = net.sqlcipher.database.SQLiteDatabase.openDatabase(
            dbFile.absolutePath,
            zeroKey,
            null,   // cursor factory
            net.sqlcipher.database.SQLiteDatabase.OPEN_READWRITE,
            null,   // hook
            null,   // errorHandler
        )
        try {
            rekeyRaw(db, correctKey)
            Log.i("ZeroKeyMigration", "Legacy zero-key database re-encrypted successfully.")
        } finally {
            db.close()
        }
    } catch (_: Exception) {
        // Database is not zero-keyed — this is the expected path for properly encrypted
        // databases and for every unlock after a successful migration.
    } finally {
        correctKey.fill(0)
    }
}

/**
 * Creates the encrypted [PeriodDatabase] and zeroizes the derived key immediately afterward.
 *
 * **Legacy migration:** Before creating the Room database, calls [migrateLegacyZeroKeyIfNeeded]
 * to transparently re-encrypt any database that was created with the all-zeros-key bug
 * (pre-`copyOf()` fix). See that function's KDoc for details.
 *
 * **Why `copyOf()`?** [PeriodDatabase.create] passes the key to SQLCipher's [SupportFactory],
 * which stores a **reference** (not a copy). `Room.databaseBuilder().build()` returns
 * *without* opening the database. If we zeroed the original key before the database was
 * actually opened, [SupportFactory] would read an all-zeros array and every passphrase
 * would succeed. Passing `key.copyOf()` gives [SupportFactory] its own array so the
 * original can be zeroed immediately. The copy is zeroed by SQLCipher's built-in
 * `clearPassphrase` mechanism when [db.openHelper.writableDatabase] is called later.
 *
 * Uses `try/finally` to guarantee the original key [ByteArray] is filled with zeros even
 * if [PeriodDatabase.create] throws, fulfilling the security contract documented at
 * [PeriodDatabase.create].
 *
 * @param context              application context for Room.
 * @param passphraseService    service that derives the 32-byte AES key.
 * @param passphrase           the user-entered secret.
 * @param keyFingerprintHolder session-scoped holder that stores a SHA-256 fingerprint of the
 *                             derived key for later verification during passphrase changes.
 * @return the [PeriodDatabase]; caller must call [db.openHelper.writableDatabase] to
 *         force-open with the real key before using DAOs.
 */
internal fun createDatabaseAndZeroizeKey(
    context: Context,
    passphraseService: PassphraseService,
    passphrase: String,
    keyFingerprintHolder: KeyFingerprintHolder,
): PeriodDatabase {
    val key = passphraseService.deriveKey(passphrase)
    return try {
        keyFingerprintHolder.store(key)
        migrateLegacyZeroKeyIfNeeded(context, key.copyOf())
        PeriodDatabase.create(context, key.copyOf())
    } finally {
        key.fill(0)
    }
}

/**
 * Root Koin module defining two DI lifetimes for the application.
 *
 * **Singleton scope** (lives for the app process):
 * [SaltStorage], [AppSettings], [SessionBus], [PassphraseService], [LockedWaterDraft],
 * [ReminderScheduler], [InsightEngine], [SessionManager], [WaterTrackerViewModel],
 * [PassphraseViewModel], [SettingsViewModel], [HintPreferences], [EducationalContentProvider].
 *
 * **Session scope** ([SESSION_SCOPE], created on unlock, destroyed on logout/autolock):
 * [PeriodDatabase], all DAOs, [PeriodRepository], use cases, library providers,
 * [TrackerViewModel], [DailyLogViewModel], [InsightsViewModel].
 *
 * **Key lifecycle:** On session creation the user's passphrase is passed to
 * [createDatabaseAndZeroizeKey], which derives the AES-256 key via Argon2id and
 * zeroizes the original key array immediately (the copy is consumed by SQLCipher).
 * When the session scope is destroyed (logout or autolock), all scoped instances —
 * including the [PeriodDatabase] — are released and the encryption key is no longer
 * reachable in memory.
 */
val appModule = module {
    single { SaltStorage(androidContext()) }

    single { AppSettings(androidContext()) }

    single { SessionBus() }

    single<PassphraseService> { PassphraseServiceAndroid(get()) }

    factory { InsightScorer() }

    factory { DataReadinessCalculator() }

    factory { ChartDataGenerator() }

    factory { CorrelationEngine() }

    factory {
        InsightEngine(
            generators = listOf(
                CycleLengthAverageGenerator(),
                NextPeriodPredictionGenerator(),
                SymptomRecurrenceGenerator(),
                MoodPhasePatternGenerator(),
                CycleLengthTrendGenerator(),
                SymptomPhasePatternGenerator(),
                EnergyPhasePatternGenerator(),
                LibidoPhasePatternGenerator(),
                FlowPatternGenerator(),
                WaterIntakePhasePatternGenerator(),
                SymptomSeverityTrendGenerator(),
                CycleSummaryGenerator(),
                CrossVariableCorrelationGenerator(get()),
            ),
            scorer = get(),
        )
    }

    single { LockedWaterDraft(androidContext()) }

    single { ReminderScheduler(androidContext()) }

    single<EducationalContentProvider> {
        StaticEducationalContentProvider(EducationalContentLoader.load(androidContext()))
    }

    single { HintPreferences(androidContext()) }

    single { DeleteAllDataUseCase(androidContext(), get(), get(), get(), get(), get()) }

    single { SessionManager(appSettings = get(), lockedWaterDraft = get()) }

    viewModel { WaterTrackerViewModel(lockedWaterDraft = get()) }

    viewModel { PassphraseViewModel(appSettings = get(), sessionManager = get()) }

    viewModel {
        SettingsViewModel(
            appSettings = get(),
            reminderScheduler = get(),
            educationalContentProvider = get(),
            hintPreferences = get(),
            deleteAllDataUseCase = get(),
            sessionManager = get(),
        )
    }

    scope(SESSION_SCOPE) {
        /*
         * --- Key Fingerprint Holder ---
         *
         * Session-scoped holder for a SHA-256 fingerprint of the derived encryption key.
         * Used to verify the current passphrase during the "Change Passphrase" flow
         * without opening a second raw SQLCipher connection.
         */
        scoped { KeyFingerprintHolder() }

        /*
         * --- Encrypted Database Provider ---
         *
         * The `passphrase: String` parameter is supplied by the ViewModel via
         * `parametersOf(passphrase)` when it resolves `PeriodDatabase` from this scope.
         *
         * `createDatabaseAndZeroizeKey` derives the 32-byte AES key, stores its
         * SHA-256 fingerprint in the [KeyFingerprintHolder], passes `key.copyOf()`
         * to `SupportFactory` (so the factory has its own array), and zeros the
         * original immediately. See `createDatabaseAndZeroizeKey` KDoc for the full
         * rationale on `copyOf()`.
         *
         * IMPORTANT: The returned database is NOT yet opened — `Room.build()` defers
         * the file open. The caller (`PassphraseViewModel.unlock()`) MUST call
         * `db.openHelper.writableDatabase` before using any DAOs, so that SQLCipher
         * consumes the real key and validates the passphrase.
         */
        scoped { (passphrase: String) ->
            createDatabaseAndZeroizeKey(androidContext(), get(), passphrase, get())
        }

        /*
         * --- DAO Providers ---
         *
         * Each DAO is resolved via `get<PeriodDatabase>()`, which returns the cached
         * (already-created) instance from the scoped provider above. Because the
         * database is scoped, all DAOs share the same `PeriodDatabase` instance
         * within a session and are destroyed together when the session scope closes.
         */
        scoped { get<PeriodDatabase>().periodDao() }
        scoped { get<PeriodDatabase>().dailyEntryDao() }
        scoped { get<PeriodDatabase>().symptomDao() }
        scoped { get<PeriodDatabase>().medicationDao() }
        scoped { get<PeriodDatabase>().medicationLogDao() }
        scoped { get<PeriodDatabase>().symptomLogDao() }
        scoped { get<PeriodDatabase>().periodLogDao() }
        scoped { get<PeriodDatabase>().waterIntakeDao() }

        // Repository Provider
        scoped<PeriodRepository> {
            RoomPeriodRepository(
                db = get(),
                periodDao = get(),
                dailyEntryDao = get(),
                symptomDao = get(),
                medicationDao = get(),
                medicationLogDao = get(),
                symptomLogDao = get(),
                periodLogDao = get(),
                waterIntakeDao = get(),
            )
        }

        // --- LIBRARY PROVIDERS ---
        scoped { SymptomLibraryProvider(get()) }
        scoped { MedicationLibraryProvider(get()) }

        // Use Case Providers
        scoped { GetOrCreateDailyLogUseCase(get()) }
        scoped { DebugSeederUseCase(get()) }
        scoped { TutorialSeederUseCase(get()) }
        scoped { TutorialCleanupUseCase(get()) }
        scoped { AutoCloseOngoingPeriodUseCase(get()) }
        // ViewModel Providers
        viewModel {
            TrackerViewModel(
                periodRepository = get(),
                symptomLibraryProvider = get(),
                medicationLibraryProvider = get(),
                autoClosePeriodUseCase = get(),
                appSettings = get(),
                educationalContentProvider = get(),
            )
        }

        viewModel { (date: LocalDate) ->
            DailyLogViewModel(
                entryDate = date,
                periodRepository = get(),
                getOrCreateDailyLog = get(),
                symptomLibraryProvider = get(),
                medicationLibraryProvider = get(),
                educationalContentProvider = get(),
            )
        }

        viewModel {
            InsightsViewModel(
                periodRepository = get(),
                insightEngine = get(),
                appSettings = get(),
                educationalContentProvider = get(),
                dataReadinessCalculator = get(),
                chartDataGenerator = get(),
            )
        }
    }
}
