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
import androidx.compose.runtime.getValue
import com.veleda.cyclewise.ui.settings.SettingsScreen
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.compose.rememberNavController
import com.veleda.cyclewise.session.SessionBus
import org.koin.compose.koinInject

@Composable
@Preview
fun CycleWiseAppUI() {
    val navController = rememberNavController()
    val sessionBus: SessionBus = koinInject()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    LaunchedEffect(Unit) {
        sessionBus.logout.collect {
            // Clear back stack and go to Passphrase screen
            navController.navigate(NavRoute.Passphrase.route) {
                popUpTo(0) { inclusive = true }
                launchSingleTop = true
                restoreState = false
            }
        }
    }

    Scaffold(
        bottomBar = {
            if (currentRoute != NavRoute.Passphrase.route) {
                BottomNavBar(navController) }
            },
        modifier = Modifier
            .fillMaxSize()
            .padding(WindowInsets.systemBars.asPaddingValues())
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = NavRoute.Passphrase.route,
            modifier = Modifier.padding(padding)
        ) {
            // 1) Passphrase entry
            composable(NavRoute.Passphrase.route) {
                PassphraseScreen {
                    navController.navigate(NavRoute.Tracker.route) {
                        popUpTo(NavRoute.Passphrase.route) { inclusive = true }
                    }
                }
            }
            // 2) Tracker
            composable(NavRoute.Tracker.route) {
                TrackerScreen(navController)
            }
            // 3) Settings placeholder
            composable(NavRoute.Settings.route) {
                SettingsScreen(navController)
            }

            composable(NavRoute.Hello.route) {
                Text("Hello!")
            }
        }
    }
}
