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
import com.veleda.cyclewise.domain.usecases.EndCycleUseCase
import com.veleda.cyclewise.settings.AppSettings
import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.koin.core.scope.Scope
import com.veleda.cyclewise.session.SessionBus

val SESSION_SCOPE = named("UnlockedSessionScope")

val appModule = module {
    // Persistent salt for Argon2
    single { SaltStorage(androidContext()) }

    // KDF: Argon2 passphrase service
    single<PassphraseService> { PassphraseServiceAndroid(get()) }

    // App scoped settings
    single { AppSettings(androidContext()) }

    // Event bus for logout/navigation signals
    single { SessionBus() }

    // Session-scoped SQLCipher DB and related services
    scope(SESSION_SCOPE) {
        scoped { (passphrase: String) ->
            val key = get<PassphraseService>().deriveKey(passphrase)
            CycleDatabase.create(androidContext(), key)
        }

        scoped<CycleRepository> { RoomCycleRepository(get<CycleDatabase>().cycleDao()) }

        scoped { StartNewCycleUseCase(get()) }

        scoped { EndCycleUseCase(get()) }

        scoped { CycleViewModel(
            get(),
            get(),
            get())
        }
    }
}
