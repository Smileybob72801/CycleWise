package com.veleda.cyclewise.ui.nav

import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState

/** Reduced opacity applied to bottom nav items when [enabled] is `false`. */
private const val DISABLED_ALPHA = 0.38f

/**
 * Bottom navigation bar displaying the three primary app tabs.
 *
 * Icons are driven by the [NavRoute.selectedIcon] and [NavRoute.unselectedIcon] metadata,
 * rendering a filled variant when the tab is active and an outlined variant when inactive.
 * Each item carries a `testTag` of `"bottom-nav-{route}"` for UI-test targeting.
 *
 * @param navController The [NavController] used to observe the current destination and
 *   perform tab navigation with state restoration.
 * @param enabled When `false`, navigation items are visible but non-interactive, with
 *   reduced opacity to signal the disabled state. Used during the tutorial walkthrough
 *   to prevent users from navigating away before cleanup runs.
 */
@Composable
fun BottomNavBar(navController: NavController, enabled: Boolean = true) {
    val navBackStackEntry = navController.currentBackStackEntryAsState().value
    val currentRoute = navBackStackEntry?.destination?.route

    NavigationBar(
        tonalElevation = 3.dp,
        modifier = if (enabled) Modifier else Modifier.alpha(DISABLED_ALPHA),
    ) {
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
                enabled = enabled,
                onClick = {
                    if (enabled && currentRoute != route.route) {
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
