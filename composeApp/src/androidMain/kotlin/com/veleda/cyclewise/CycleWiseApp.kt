package com.veleda.cyclewise

import android.app.Application
import android.content.SharedPreferences
import android.os.SystemClock
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.veleda.cyclewise.di.appModule
import com.veleda.cyclewise.settings.AppSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import androidx.core.content.edit
import com.veleda.cyclewise.session.SessionBus

class CycleWiseApp :
    Application(),
    LifecycleEventObserver,
    KoinComponent {

    private lateinit var prefs: SharedPreferences
    private val appSettings: AppSettings by inject()
    private val sessionBus: SessionBus by inject()

    // Background scope to keep an autolock cache fresh
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // Default if settings not available yet
    @Volatile
    private var autolockMinutesCache: Int = 10

    override fun onCreate() {
        super.onCreate()

        startKoin {
            printLogger()
            androidContext(this@CycleWiseApp)
            modules(appModule)
            allowOverride(false)
        }

        prefs = getSharedPreferences("autolock_prefs", MODE_PRIVATE)

        // Keep a live cache of the autolock minutes (avoids blocking on foreground)
        appScope.launch {
            appSettings.autolockMinutes.collectLatest { minutes ->
                autolockMinutesCache = minutes
            }
        }

        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        when (event) {
            Lifecycle.Event.ON_STOP -> {
                // App moved to background — remember when
                prefs.edit {
                    putLong(KEY_LAST_BG_AT_ELAPSED, SystemClock.elapsedRealtime())
                }
            }
            Lifecycle.Event.ON_START -> {
                // App returned to foreground — decide if we must lock immediately
                val minutes = autolockMinutesCache
                val last = prefs.getLong(KEY_LAST_BG_AT_ELAPSED, -1L)

                if (shouldLockNow(minutes, last)) {
                    // Close session scope to lock DB + clear session-scoped VMs
                    getKoin().getScopeOrNull(SESSION_SCOPE_ID)?.close()

                    // Hardening: clear the timestamp to avoid loop/stale values
                    prefs.edit { remove(KEY_LAST_BG_AT_ELAPSED) }

                    // Notify UI to navigate to login immediately
                    sessionBus.emitLogout()
                }
            }
            else -> Unit
        }
    }

    /**
     * Hardening rules:
     * - minutes == 0  -> always lock on foreground
     * - no last value -> don't lock (unless minutes == 0)
     * - elapsed >= minutes -> lock
     */
    private fun shouldLockNow(minutes: Int, lastBgAtElapsed: Long): Boolean {
        if (minutes == 0) return true
        if (lastBgAtElapsed <= 0L) return false
        val elapsedMs = SystemClock.elapsedRealtime() - lastBgAtElapsed
        val thresholdMs = minutes * 60_000L
        return elapsedMs >= thresholdMs
    }

    companion object {
        private const val KEY_LAST_BG_AT_ELAPSED = "last_bg_at_elapsed"
        private const val SESSION_SCOPE_ID = "session"
    }
}
