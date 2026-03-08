package com.veleda.cyclewise.ui.nav

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.datetime.LocalDate

/**
 * Sealed hierarchy of all navigation routes in the app.
 *
 * Each route carries its navigation path, a human-readable label, and optional icon
 * metadata for bottom-navigation rendering. Routes that appear in the bottom bar
 * provide [selectedIcon] (filled variant) and [unselectedIcon] (outlined variant);
 * non-bottom-bar routes leave both as `null`.
 *
 * @property route The Navigation-Compose route pattern (may contain path/query parameters).
 * @property label Display name used in the bottom navigation bar and screen titles.
 * @property selectedIcon Filled icon shown when the tab is active, or `null` for non-tab routes.
 * @property unselectedIcon Outlined icon shown when the tab is inactive, or `null` for non-tab routes.
 */
sealed class NavRoute(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector? = null,
    val unselectedIcon: ImageVector? = null,
) {
    /** Passphrase authentication gate — no bottom-bar icon. */
    object Passphrase : NavRoute("passphrase", "Pass Phrase")

    /**
     * Daily Log home tab — the default destination after unlock.
     *
     * Renders [DailyLogScreen] for today's date. Unlike [DailyLog] (the detail route
     * navigated from Tracker), this route has no date parameter and always shows today.
     */
    object DailyLogHome : NavRoute(
        route = "daily_log_home",
        label = "Daily Log",
        selectedIcon = Icons.Filled.EditNote,
        unselectedIcon = Icons.Outlined.EditNote,
    )

    /** Cycle tracker calendar tab. */
    object Tracker : NavRoute(
        route = "tracker",
        label = "Tracker",
        selectedIcon = Icons.Filled.CalendarMonth,
        unselectedIcon = Icons.Outlined.CalendarMonth,
    )

    /** Insights analytics tab. */
    object Insights : NavRoute(
        route = "insights",
        label = "Insights",
        selectedIcon = Icons.Filled.Insights,
        unselectedIcon = Icons.Outlined.Insights,
    )

    /** App settings tab. */
    object Settings : NavRoute(
        route = "settings",
        label = "Settings",
        selectedIcon = Icons.Filled.Settings,
        unselectedIcon = Icons.Outlined.Settings,
    )

    /** Daily log detail screen — navigated from Tracker for a specific date. No bottom-bar icon. */
    object DailyLog : NavRoute("log/{date}", "Daily Log") {
        /** Builds a concrete route for the given [date]. */
        fun createRoute(date: LocalDate) = "log/$date"
    }

    companion object {
        /** Routes displayed in the bottom navigation bar. */
        val all: List<NavRoute>
            get() = listOf(DailyLogHome, Tracker, Insights, Settings)
    }
}
