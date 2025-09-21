package com.veleda.cyclewise

import android.app.Application

/**
 * A custom Application class for tests that does NOTHING.
 * This prevents the real `startKoin` in the production app from being called
 * during tests, giving our KoinTestRule full control.
 */
class RobolectricTestApp : Application() {
    override fun onCreate() {
        // We keep this empty on purpose to override the production app's initialization.
        super.onCreate()
    }
}