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
        // Ensure a clean Koin state
        stopKoin()

        startKoin {
            androidContext(app)
            allowOverride(true)
            modules(appModule, testOverridesModule)
        }
        super.callApplicationOnCreate(app)
    }
}