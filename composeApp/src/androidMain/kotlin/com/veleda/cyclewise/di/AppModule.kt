package com.veleda.cyclewise.di

import com.veleda.cyclewise.domain.repository.PeriodRepository
import com.veleda.cyclewise.services.SaltStorage
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
import com.veleda.cyclewise.ui.insights.InsightsViewModel
import kotlinx.datetime.LocalDate
import org.koin.core.qualifier.Qualifier

val SESSION_SCOPE: Qualifier = named("UnlockedSessionScope")

val appModule = module {
    // Persistent salt for Argon2
    single { SaltStorage(androidContext()) }

    single { AppSettings(androidContext()) }

    single { SessionBus() }

    // KDF: Argon2 passphrase service
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

    // PassphraseViewModel here, outside of any scope
    viewModel { PassphraseViewModel(appSettings = get()) }

    // Session-scoped SQLCipher DB and related services
    scope(SESSION_SCOPE) {
        // DB Provider
        scoped { (passphrase: String) ->
            val key = get<PassphraseService>().deriveKey(passphrase)
            PeriodDatabase.create(androidContext(), key)
        }

        // DAO Providers
        scoped { get<PeriodDatabase>().periodDao() }
        scoped { get<PeriodDatabase>().dailyEntryDao() }
        scoped { get<PeriodDatabase>().symptomDao() }
        scoped { get<PeriodDatabase>().medicationDao() }
        scoped { get<PeriodDatabase>().medicationLogDao() }
        scoped { get<PeriodDatabase>().symptomLogDao() }
        scoped { get<PeriodDatabase>().periodLogDao() }

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
                autoClosePeriodUseCase = get()
            )
        }

        viewModel { (date: LocalDate, isPeriodDay: Boolean) ->
            DailyLogViewModel(
                entryDate = date,
                periodRepository = get(),
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
