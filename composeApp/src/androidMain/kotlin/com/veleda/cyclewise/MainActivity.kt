package com.veleda.cyclewise

import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.veleda.cyclewise.ui.CycleWiseAppUI

/**
 * Single-activity entry point for the RhythmWise application.
 *
 * Configures three security/UX concerns before setting the Compose content:
 * - **FLAG_SECURE** — prevents screenshots and the recent-apps thumbnail from
 *   exposing sensitive health data.
 * - **Global crash handler** — logs uncaught exceptions via [Log.e] so crash
 *   details are available in logcat without a remote crash-reporting service.
 * - **Splash screen** — integrates the AndroidX SplashScreen API for a seamless
 *   cold-start transition.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e("GlobalCrashHandler", "Uncaught exception in ${thread.name}: ${throwable.message}")
        }

        setContent {
            CycleWiseAppUI()
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    CycleWiseAppUI()
}