package com.veleda.cyclewise.ui.nav

sealed class NavRoute(val route: String, val label: String) {
    object Hello : NavRoute("hello", "Hello")
    object Tracker : NavRoute("tracker", "Tracker")
    object Settings : NavRoute("settings", "Settings")

    companion object {
        val all = listOf(Hello, Tracker, Settings)
    }
}