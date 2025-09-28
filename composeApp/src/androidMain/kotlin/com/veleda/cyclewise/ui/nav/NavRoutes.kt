package com.veleda.cyclewise.ui.nav

import kotlinx.datetime.LocalDate

sealed class NavRoute(val route: String, val label: String) {
    object Passphrase : NavRoute("passphrase", "Pass Phrase")
    object Tracker : NavRoute("tracker", "Tracker")
    object Insights : NavRoute("insights", "Insights")
    object Settings : NavRoute("settings", "Settings")
    object DailyLog : NavRoute("log/{date}", "Daily Log") {
        fun createRoute(date: LocalDate) = "log/$date"
    }

    companion object {
        val all = listOf(
            Tracker,
            Insights,
            Settings)
    }
}