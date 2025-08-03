package com.veleda.cyclewise.ui.nav

sealed class NavRoute(val route: String, val label: String) {
    object Hello : NavRoute("hello", "Hello")

    object Passphrase : NavRoute("passphrase", "Pass Phrase")
    object Tracker : NavRoute("tracker/{passphrase}", "Tracker") {
        fun createRoute(passphrase: String) = "tracker/$passphrase"
    }
    object Settings : NavRoute("settings", "Settings")

    companion object {
        val all = listOf(Hello, Tracker, Settings)
    }
}