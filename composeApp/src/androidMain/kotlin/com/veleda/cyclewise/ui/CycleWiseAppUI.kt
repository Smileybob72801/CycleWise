package com.veleda.cyclewise.ui

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.veleda.cyclewise.ui.auth.PassphraseScreen
import com.veleda.cyclewise.ui.insights.InsightsScreen
import com.veleda.cyclewise.ui.log.DailyLogScreen
import com.veleda.cyclewise.ui.nav.BottomNavBar
import com.veleda.cyclewise.ui.nav.NavRoute
import com.veleda.cyclewise.ui.settings.SettingsScreen
import com.veleda.cyclewise.ui.settings.SettingsViewModel
import com.veleda.cyclewise.ui.theme.RhythmWiseTheme
import com.veleda.cyclewise.ui.theme.ThemeMode
import com.veleda.cyclewise.ui.tracker.TrackerScreen
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.androidx.compose.koinViewModel
import kotlin.time.Clock

/** Duration (ms) for default bottom-nav tab crossfade transitions. */
private const val DEFAULT_TRANSITION_DURATION_MS = 300

/** Duration (ms) for the DailyLog detail slide transition. */
private const val DETAIL_TRANSITION_DURATION_MS = 300

/** Duration (ms) for the passphrase authentication gate fade transition. */
private const val AUTH_TRANSITION_DURATION_MS = 400

@Composable
@Preview
fun CycleWiseAppUI() {
    val settingsViewModel: SettingsViewModel = koinViewModel()
    val settingsState by settingsViewModel.uiState.collectAsState()
    val darkTheme = when (settingsState.themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    RhythmWiseTheme(darkTheme = darkTheme) {
        val navController = rememberNavController()
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        Scaffold(
            bottomBar = {
                if (currentRoute != NavRoute.Passphrase.route) {
                    BottomNavBar(navController)
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(WindowInsets.systemBars.asPaddingValues()),
        ) { padding ->
            NavHost(
                navController = navController,
                startDestination = NavRoute.Passphrase.route,
                modifier = Modifier.padding(padding),
                enterTransition = { fadeIn(tween(DEFAULT_TRANSITION_DURATION_MS)) },
                exitTransition = { fadeOut(tween(DEFAULT_TRANSITION_DURATION_MS)) },
                popEnterTransition = { fadeIn(tween(DEFAULT_TRANSITION_DURATION_MS)) },
                popExitTransition = { fadeOut(tween(DEFAULT_TRANSITION_DURATION_MS)) },
            ) {
                composable(
                    route = NavRoute.Passphrase.route,
                    enterTransition = { fadeIn(tween(AUTH_TRANSITION_DURATION_MS)) },
                    exitTransition = { fadeOut(tween(AUTH_TRANSITION_DURATION_MS)) },
                    popEnterTransition = { EnterTransition.None },
                    popExitTransition = { fadeOut(tween(AUTH_TRANSITION_DURATION_MS)) },
                ) {
                    PassphraseScreen {
                        navController.navigate(NavRoute.DailyLogHome.route) {
                            popUpTo(NavRoute.Passphrase.route) { inclusive = true }
                        }
                    }
                }
                composable(NavRoute.DailyLogHome.route) {
                    val today = remember { Clock.System.todayIn(TimeZone.currentSystemDefault()) }
                    DailyLogScreen(
                        date = today,
                        onNavigateToTracker = {
                            navController.navigate(NavRoute.Tracker.route) {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                    )
                }
                composable(NavRoute.Tracker.route) {
                    TrackerScreen(navController)
                }
                composable(NavRoute.Insights.route) {
                    InsightsScreen()
                }
                composable(NavRoute.Settings.route) {
                    SettingsScreen(navController)
                }

                composable(
                    route = NavRoute.DailyLog.route,
                    arguments = listOf(
                        navArgument("date") { type = NavType.StringType },
                    ),
                    enterTransition = {
                        slideInVertically(
                            animationSpec = tween(DETAIL_TRANSITION_DURATION_MS),
                            initialOffsetY = { fullHeight -> fullHeight },
                        )
                    },
                    exitTransition = { fadeOut(tween(DETAIL_TRANSITION_DURATION_MS)) },
                    popEnterTransition = { fadeIn(tween(DETAIL_TRANSITION_DURATION_MS)) },
                    popExitTransition = {
                        slideOutVertically(
                            animationSpec = tween(DETAIL_TRANSITION_DURATION_MS),
                            targetOffsetY = { fullHeight -> fullHeight },
                        )
                    },
                ) { backStackEntry ->
                    val dateString = backStackEntry.arguments?.getString("date")
                    if (dateString != null) {
                        DailyLogScreen(
                            date = LocalDate.parse(dateString),
                        )
                    }
                }
            }
        }
    }
}
