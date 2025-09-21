package com.veleda.cyclewise

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner
import com.veleda.cyclewise.di.appModule
import com.veleda.cyclewise.di.testOverridesModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin

/**
 * A custom test runner that prepares a clean, isolated environment for E2E tests.
 * It cleans up persistent state and initializes Koin with test-specific overrides.
 */
@Suppress("unused")
class CustomTestRunner : AndroidJUnitRunner() {

    override fun newApplication(cl: ClassLoader?, name: String?, context: Context?): Application {
        return super.newApplication(cl, TestCycleWiseApp::class.java.name, context)
    }

    override fun callApplicationOnCreate(app: Application) {
        // Wipe BOTH possible DBs in the AUT sandbox
        val appCtx = app.applicationContext
        val testDbName = "e2e_cyclewise.db"
        val prodDbName = "cyclewise.db"

        listOf(
            testDbName, "$testDbName-shm", "$testDbName-wal",
            prodDbName, "$prodDbName-shm", "$prodDbName-wal"
        ).forEach { appCtx.getDatabasePath(it).delete() }

        // Wipe the Argon2 salt in the AUT sandbox
        appCtx.getSharedPreferences("cyclewise_salt_prefs", Context.MODE_PRIVATE)
            .edit().clear().apply()

        stopKoin()

        // --- 2. Start Koin with Overrides ---
        startKoin {
            androidContext(app)
            allowOverride(true)
            // Load production modules first, then our test overrides.
            // Koin will replace the production database with the test one.
            modules(appModule, testOverridesModule)
        }

        super.callApplicationOnCreate(app)
    }
}