package com.veleda.cyclewise.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.navigation.compose.rememberNavController
import com.veleda.cyclewise.ui.screens.HelloScreen
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.navigation.compose.*
import com.veleda.cyclewise.ui.nav.*
import com.veleda.cyclewise.ui.screens.HelloScreen
import com.veleda.cyclewise.ui.tracker.TrackerScreen

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
            composable(NavRoute.Hello.route) { HelloScreen() }
            composable(NavRoute.Tracker.route) { TrackerScreen() }
            composable(NavRoute.Settings.route) { Text("Settings screen coming soon") }
        }
    }
}
