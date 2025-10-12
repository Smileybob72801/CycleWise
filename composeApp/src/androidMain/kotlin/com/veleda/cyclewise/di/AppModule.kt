package com.veleda.cyclewise.di

import com.veleda.cyclewise.domain.repository.CycleRepository
import com.veleda.cyclewise.domain.usecases.StartNewCycleUseCase
import com.veleda.cyclewise.services.SaltStorage
import com.veleda.cyclewise.ui.tracker.CycleViewModel
import org.koin.core.module.dsl.*
import org.koin.dsl.module
import org.koin.android.ext.koin.androidContext
import org.koin.core.parameter.parametersOf
import com.veleda.cyclewise.domain.services.PassphraseService
import com.veleda.cyclewise.services.PassphraseServiceAndroid
import com.veleda.cyclewise.androidData.local.database.CycleDatabase
import com.veleda.cyclewise.androidData.repository.RoomCycleRepository
import com.veleda.cyclewise.domain.insights.InsightEngine
import com.veleda.cyclewise.domain.insights.generators.CycleLengthAverageGenerator
import com.veleda.cyclewise.domain.insights.generators.CycleLengthTrendGenerator
import com.veleda.cyclewise.domain.insights.generators.InsightGenerator
import com.veleda.cyclewise.domain.insights.generators.MoodPhasePatternGenerator
import com.veleda.cyclewise.domain.insights.generators.NextPeriodPredictionGenerator
import com.veleda.cyclewise.domain.insights.generators.SymptomPhasePatternGenerator
import com.veleda.cyclewise.domain.insights.generators.SymptomRecurrenceGenerator
import com.veleda.cyclewise.domain.providers.MedicationLibraryProvider
import com.veleda.cyclewise.domain.providers.SymptomLibraryProvider
import com.veleda.cyclewise.domain.usecases.DebugSeederUseCase
import com.veleda.cyclewise.domain.usecases.EndCycleUseCase
import org.koin.core.qualifier.named
import com.veleda.cyclewise.domain.usecases.GetOrCreateDailyEntryUseCase
import com.veleda.cyclewise.session.SessionBus
import com.veleda.cyclewise.ui.log.DailyLogViewModel
import com.veleda.cyclewise.settings.AppSettings
import com.veleda.cyclewise.ui.auth.PassphraseViewModel
import com.veleda.cyclewise.ui.insights.InsightsViewModel
import org.koin.dsl.module
import org.koin.core.scope.Scope
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
            CycleDatabase.create(androidContext(), key)
        }

        // DAO Providers
        scoped { get<CycleDatabase>().cycleDao() }
        scoped { get<CycleDatabase>().dailyEntryDao() }
        scoped { get<CycleDatabase>().symptomDao() }
        scoped { get<CycleDatabase>().medicationDao() }
        scoped { get<CycleDatabase>().medicationLogDao() }
        scoped { get<CycleDatabase>().symptomLogDao() }

        // Repository Provider
        scoped<CycleRepository> {
            RoomCycleRepository(
                db = get(),
                cycleDao = get(),
                dailyEntryDao = get(),
                symptomDao = get(),
                medicationDao = get(),
                medicationLogDao = get(),
                symptomLogDao = get(),
            )
        }

        // --- LIBRARY PROVIDERS ---
        scoped { SymptomLibraryProvider(get()) }
        scoped { MedicationLibraryProvider(get()) }

        // Use Case Providers
        scoped { GetOrCreateDailyEntryUseCase(get()) }
        scoped { DebugSeederUseCase(get()) }

        // ViewModel Providers
        viewModel {
            CycleViewModel(
                cycleRepository = get(),
                symptomLibraryProvider = get(),
                medicationLibraryProvider = get()
            )
        }

        viewModel { (date: LocalDate) ->
            DailyLogViewModel(
                entryDate = date,
                cycleRepository = get(),
                symptomLibraryProvider = get(),
                medicationLibraryProvider = get()
            )
        }

        viewModel {
            InsightsViewModel(
                cycleRepository = get(),
                insightEngine = get(),
                appSettings = get()
            )
        }
    }
}
