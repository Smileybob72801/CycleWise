package com.veleda.cyclewise.ui.nav

import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState

/**
 * Bottom navigation bar displaying the three primary app tabs.
 *
 * Icons are driven by the [NavRoute.selectedIcon] and [NavRoute.unselectedIcon] metadata,
 * rendering a filled variant when the tab is active and an outlined variant when inactive.
 * Each item carries a `testTag` of `"bottom-nav-{route}"` for UI-test targeting.
 *
 * @param navController The [NavController] used to observe the current destination and
 *   perform tab navigation with state restoration.
 */
@Composable
fun BottomNavBar(navController: NavController) {
    val navBackStackEntry = navController.currentBackStackEntryAsState().value
    val currentRoute = navBackStackEntry?.destination?.route

    NavigationBar(tonalElevation = 3.dp) {
        NavRoute.all.forEach { route ->
            val selected = currentRoute == route.route
            val icon = if (selected) route.selectedIcon else route.unselectedIcon

            NavigationBarItem(
                modifier = Modifier.testTag("bottom-nav-${route.route}"),
                icon = {
                    if (icon != null) {
                        Icon(
                            imageVector = icon,
                            contentDescription = route.label,
                        )
                    }
                },
                label = { Text(route.label) },
                selected = selected,
                onClick = {
                    if (currentRoute != route.route) {
                        navController.navigate(route.route) {
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
            )
        }
    }
}
