package com.veleda.cyclewise

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.veleda.cyclewise.ui.CycleWiseAppUI

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e("GlobalCrashHandler", "Uncaught exception in ${thread.name}", throwable)
            throwable.printStackTrace()
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