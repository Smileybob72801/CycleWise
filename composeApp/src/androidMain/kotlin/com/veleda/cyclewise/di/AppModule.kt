package com.veleda.cyclewise.di

import com.veleda.cyclewise.androidData.local.draft.LockedWaterDraft
import android.content.Context
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
import com.veleda.cyclewise.androidData.repository.RoomPeriodRepository
import com.veleda.cyclewise.domain.insights.InsightEngine
import com.veleda.cyclewise.domain.insights.generators.CycleLengthAverageGenerator
import com.veleda.cyclewise.domain.insights.generators.CycleLengthTrendGenerator
import com.veleda.cyclewise.domain.insights.generators.MoodPhasePatternGenerator
import com.veleda.cyclewise.domain.insights.generators.NextPeriodPredictionGenerator
import com.veleda.cyclewise.domain.insights.generators.SymptomPhasePatternGenerator
import com.veleda.cyclewise.domain.insights.generators.SymptomRecurrenceGenerator
import com.veleda.cyclewise.domain.providers.MedicationLibraryProvider
import com.veleda.cyclewise.domain.providers.SymptomLibraryProvider
import com.veleda.cyclewise.domain.usecases.AutoCloseOngoingPeriodUseCase
import com.veleda.cyclewise.domain.usecases.DebugSeederUseCase
import org.koin.core.qualifier.named
import com.veleda.cyclewise.domain.usecases.GetOrCreateDailyLogUseCase
import com.veleda.cyclewise.session.SessionBus
import com.veleda.cyclewise.ui.log.DailyLogViewModel
import com.veleda.cyclewise.settings.AppSettings
import com.veleda.cyclewise.ui.auth.PassphraseViewModel
import com.veleda.cyclewise.reminders.ReminderScheduler
import com.veleda.cyclewise.ui.insights.InsightsViewModel
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
 * Creates the encrypted [PeriodDatabase] and zeroizes the derived key immediately afterward.
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
 * @param context          application context for Room.
 * @param passphraseService service that derives the 32-byte AES key.
 * @param passphrase       the user-entered secret.
 * @return the [PeriodDatabase]; caller must call [db.openHelper.writableDatabase] to
 *         force-open with the real key before using DAOs.
 */
internal fun createDatabaseAndZeroizeKey(
    context: Context,
    passphraseService: PassphraseService,
    passphrase: String
): PeriodDatabase {
    val key = passphraseService.deriveKey(passphrase)
    return try {
        PeriodDatabase.create(context, key.copyOf())
    } finally {
        key.fill(0)
    }
}

/**
 * Root Koin module defining two DI lifetimes:
 *
 * **Singleton scope** (lives for the app process):
 * [SaltStorage], [AppSettings], [SessionBus], [PassphraseService], [LockedWaterDraft],
 * [ReminderScheduler], [InsightEngine], [WaterTrackerViewModel], [PassphraseViewModel].
 *
 * **Session scope** ([SESSION_SCOPE], created on unlock, destroyed on logout/autolock):
 * [PeriodDatabase], all DAOs, [PeriodRepository], use cases, library providers,
 * [TrackerViewModel], [DailyLogViewModel], [InsightsViewModel].
 */
val appModule = module {
    single { SaltStorage(androidContext()) }

    single { AppSettings(androidContext()) }

    single { SessionBus() }

    single<PassphraseService> { PassphraseServiceAndroid(get()) }

    factory {
        InsightEngine(
            listOf(
                CycleLengthAverageGenerator(),
                NextPeriodPredictionGenerator(),
                SymptomRecurrenceGenerator(),
                MoodPhasePatternGenerator(),
                CycleLengthTrendGenerator(),
                SymptomPhasePatternGenerator()
            )
        )
    }

    single { LockedWaterDraft(androidContext()) }

    single { ReminderScheduler(androidContext()) }

    viewModel { WaterTrackerViewModel(lockedWaterDraft = get()) }

    viewModel { PassphraseViewModel(appSettings = get(), lockedWaterDraft = get()) }

    scope(SESSION_SCOPE) {
        // DB Provider
        scoped { (passphrase: String) ->
            createDatabaseAndZeroizeKey(androidContext(), get(), passphrase)
        }

        // DAO Providers
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
        scoped { AutoCloseOngoingPeriodUseCase(get()) }

        // ViewModel Providers
        viewModel {
            TrackerViewModel(
                periodRepository = get(),
                symptomLibraryProvider = get(),
                medicationLibraryProvider = get(),
                autoClosePeriodUseCase = get(),
                appSettings = get()
            )
        }

        viewModel { (date: LocalDate, isPeriodDay: Boolean) ->
            DailyLogViewModel(
                entryDate = date,
                periodRepository = get(),
                getOrCreateDailyLog = get(),
                symptomLibraryProvider = get(),
                medicationLibraryProvider = get(),
                isPeriodDay = isPeriodDay
            )
        }

        viewModel {
            InsightsViewModel(
                periodRepository = get(),
                insightEngine = get(),
                appSettings = get()
            )
        }
    }
}
