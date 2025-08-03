package com.veleda.cyclewise.ui

import android.util.Log
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import org.jetbrains.compose.ui.tooling.preview.Preview
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.veleda.cyclewise.ui.nav.*
import com.veleda.cyclewise.ui.tracker.TrackerScreen
import com.veleda.cyclewise.ui.auth.PassphraseScreen

@Composable
@Preview
fun CycleWiseAppUI() {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = { BottomNavBar(navController) },
        modifier = Modifier
            .fillMaxSize()
            .padding(WindowInsets.systemBars.asPaddingValues())
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = NavRoute.Hello.route,
            modifier = Modifier.padding(padding)
        ) {
            // 1) Passphrase entry
            composable(NavRoute.Passphrase.route) {
                PassphraseScreen { pass ->
                    navController.navigate(NavRoute.Tracker.createRoute(pass)) {
                        Log.d("PassphraseNav", "Navigating with passphrase: '$pass'")
                        popUpTo(NavRoute.Passphrase.route) { inclusive = true }
                    }
                }
            }
            // 2) Tracker — inject passphrase as nav arg
            composable(
                route = NavRoute.Tracker.route,
                arguments = listOf(navArgument("passphrase") {
                    type = NavType.StringType
                })
            ) { backStack ->
                val pass = backStack.arguments!!.getString("passphrase")!!
                TrackerScreen(passphrase = pass)
            }
            // 3) Settings placeholder
            composable(NavRoute.Settings.route) {
                Text("Settings screen coming soon")
            }
        }
    }
}
