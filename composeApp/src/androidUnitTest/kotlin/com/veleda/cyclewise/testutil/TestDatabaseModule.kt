package com.veleda.cyclewise.testutil

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.veleda.cyclewise.androidData.local.database.PeriodDatabase
import org.koin.dsl.module

/**
 * Shared Koin module for all DAO and repository integration tests.
 * Provides an in-memory Room database and all DAOs.
 * Use with [com.veleda.cyclewise.KoinTestRule] to avoid manual startKoin/stopKoin.
 */
val testDatabaseModule = module {
    single {
        Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            PeriodDatabase::class.java
        )
            .allowMainThreadQueries()
            .build()
    }
    single { get<PeriodDatabase>().periodDao() }
    single { get<PeriodDatabase>().dailyEntryDao() }
    single { get<PeriodDatabase>().symptomDao() }
    single { get<PeriodDatabase>().symptomLogDao() }
    single { get<PeriodDatabase>().medicationDao() }
    single { get<PeriodDatabase>().medicationLogDao() }
    single { get<PeriodDatabase>().periodLogDao() }
    single { get<PeriodDatabase>().waterIntakeDao() }
}
