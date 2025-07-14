package com.veleda.cyclewise.ui.nav

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*

@Composable
fun BottomNavBar(navController: NavController) {
    val navBackStackEntry = navController.currentBackStackEntryAsState().value
    val currentRoute = navBackStackEntry?.destination?.route

    NavigationBar {
        NavRoute.all.forEach { route ->
            NavigationBarItem(
                icon = {
                    Icon(
                        imageVector = when (route) {
                            NavRoute.Hello -> Icons.Default.Home
                            NavRoute.Tracker -> Icons.Default.DateRange
                            NavRoute.Settings -> Icons.Default.Settings
                        },
                        contentDescription = route.label
                    )
                },
                label = { Text(route.label) },
                selected = currentRoute == route.route,
                onClick = {
                    if (currentRoute != route.route) {
                        navController.navigate(route.route) {
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                }
            )
        }
    }
}