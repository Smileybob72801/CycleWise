package com.veleda.cyclewise

import android.app.Application
import com.veleda.cyclewise.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

/**
 * A custom Application class for instrumented tests.
 * Its `onCreate` method is called by the test runner BEFORE any Activity is created.
 * This is the perfect place to initialize Koin with our test-specific modules.
 */
class TestCycleWiseApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Koin initialization will now be handled by our CustomTestRunner
        // to ensure it happens with the correct test overrides.
    }
}