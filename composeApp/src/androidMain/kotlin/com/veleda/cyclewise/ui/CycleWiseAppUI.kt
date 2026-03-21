package com.veleda.cyclewise.ui

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import com.veleda.cyclewise.domain.usecases.TutorialCleanupUseCase
import com.veleda.cyclewise.session.SessionBus
import com.veleda.cyclewise.settings.AppSettings
import com.veleda.cyclewise.settings.runSeedCleanupIfNeeded
import com.veleda.cyclewise.ui.coachmark.HintKey
import com.veleda.cyclewise.ui.coachmark.HintPreferences
import com.veleda.cyclewise.ui.tutorial.shouldRunTutorialBoundsCleanup
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.getKoin
import kotlin.time.Clock

/** Duration (ms) for default bottom-nav tab crossfade transitions. */
private const val DEFAULT_TRANSITION_DURATION_MS = 300

/** Duration (ms) for the DailyLog detail slide transition. */
private const val DETAIL_TRANSITION_DURATION_MS = 300

/** Duration (ms) for the passphrase authentication gate fade transition. */
private const val AUTH_TRANSITION_DURATION_MS = 400

/** Grace period before the global tutorial bounds check fires, to avoid false positives during navigation. */
private const val TUTORIAL_BOUNDS_CHECK_DELAY_MS = 2000L

/**
 * Root composable that wires up theming, navigation host, bottom nav bar, and session logout handling.
 *
 * Collects [SessionBus.logout] to navigate back to the passphrase screen when the session
 * scope is destroyed (autolock or manual lock), clearing the entire back stack.
 */
@Composable
@Preview
fun CycleWiseAppUI() {
    val settingsViewModel: SettingsViewModel = koinViewModel()
    val appearanceState by settingsViewModel.appearanceState.collectAsState()
    val darkTheme = when (appearanceState.themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    RhythmWiseTheme(darkTheme = darkTheme) {
        val navController = rememberNavController()
        val koin = getKoin()
        val sessionBus: SessionBus = koin.get()
        val appSettings: AppSettings = koin.get()
        val seedManifestJson by appSettings.seedManifestJson.collectAsState(initial = "")
        val tutorialActive = seedManifestJson.isNotEmpty()
        val hintPreferences: HintPreferences = koin.get()

        // Autolock: navigate to passphrase screen when session is destroyed
        LaunchedEffect(Unit) {
            sessionBus.logout.collect {
                navController.navigate(NavRoute.Passphrase.route) {
                    popUpTo(0) { inclusive = true }
                }
            }
        }

        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        // Global tutorial bounds check — catches any state where seed data persists
        // but no walkthrough is actively running on the current screen.
        LaunchedEffect(tutorialActive, currentRoute) {
            if (!tutorialActive) return@LaunchedEffect
            delay(TUTORIAL_BOUNDS_CHECK_DELAY_MS)
            val dailyLogDone = hintPreferences.isHintSeen(HintKey.DAILY_LOG_NOTES_TAB).first()
            val trackerDone = hintPreferences.isHintSeen(HintKey.TRACKER_TAP_DAY).first()
            if (shouldRunTutorialBoundsCleanup(currentRoute, dailyLogDone, trackerDone)) {
                // Mark all hints seen so walkthroughs don't restart
                HintKey.entries.forEach { hintPreferences.markHintSeen(it) }
                val sessionScope = koin.getScope("session")
                val cleanup: TutorialCleanupUseCase = sessionScope.get()
                runSeedCleanupIfNeeded(appSettings, cleanup)
            }
        }

        Scaffold(
            bottomBar = {
                if (currentRoute != NavRoute.Passphrase.route) {
                    BottomNavBar(navController, enabled = !tutorialActive)
                }
            },
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            modifier = Modifier.fillMaxSize(),
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
                        onDone = {
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
                            onDone = { navController.popBackStack() },
                        )
                    }
                }
            }
        }
    }
}
