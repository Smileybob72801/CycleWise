package com.veleda.cyclewise

import android.app.Application
import com.veleda.cyclewise.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class CycleWiseApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            printLogger()
            androidContext(this@CycleWiseApp)
            modules(appModule)
            allowOverride(false)
        }
    }
}
