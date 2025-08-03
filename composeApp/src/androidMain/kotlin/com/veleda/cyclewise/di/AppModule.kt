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

val appModule = module {
    // 1) Persistent salt for Argon2
    single { SaltStorage(androidContext()) }

    // 2) KDF: Argon2 passphrase service
    single<PassphraseService> { PassphraseServiceAndroid(get()) }

    // 3) Derive raw key bytes from the passphrase + salt
    factory<ByteArray> { (passphrase: String) ->
        get<PassphraseService>().deriveKey(passphrase)
    }

    // 4) Encrypted Room database, built when we have the key
    factory { (passphrase: String) ->
        val key: ByteArray = get<ByteArray> { parametersOf(passphrase) }
        CycleDatabase.create(androidContext(), key)
    }

    // 5) Room-backed repository implementation
    factory<CycleRepository> { (passphrase: String) ->
        val db = get<CycleDatabase> { parametersOf(passphrase) }
        RoomCycleRepository(db.cycleDao())
    }

    // 6) Make the use‐case also parameterized on the same passphrase
    factory<StartNewCycleUseCase> { (passphrase: String) ->
        StartNewCycleUseCase(get<CycleRepository> { parametersOf(passphrase) })
    }

    // 7) Finally, register the ViewModel
    viewModel { (passphrase: String) ->
        CycleViewModel(
            cycleRepositoryProvider = {
                get<CycleRepository> { parametersOf(passphrase) }
            },
            startNewCycleUseCaseProvider = {
                get<StartNewCycleUseCase> { parametersOf(passphrase) }
            }
        )
    }

}
