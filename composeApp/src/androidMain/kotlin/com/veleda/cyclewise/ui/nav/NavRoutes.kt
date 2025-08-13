package com.veleda.cyclewise.ui.nav

import kotlinx.datetime.LocalDate

sealed class NavRoute(val route: String, val label: String) {
    object Hello : NavRoute("hello", "Hello")

    object Passphrase : NavRoute("passphrase", "Pass Phrase")
    object Tracker : NavRoute("tracker/{passphrase}", "Tracker") {
        fun createRoute(passphrase: String) = "tracker/$passphrase"
    }
    object Settings : NavRoute("settings", "Settings")

    object DailyLog : NavRoute("log/{date}", "Daily Log") {
        fun createRoute(date: LocalDate) = "log/$date"
    }

    companion object {
        val all = listOf(Hello, Tracker, Settings)
    }
}