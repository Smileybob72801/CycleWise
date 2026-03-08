package com.veleda.cyclewise.ui.tutorial

import com.veleda.cyclewise.ui.nav.NavRoute
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TutorialBoundsCheckTest {

    // --- Null / Passphrase route: never trigger cleanup ---

    @Test
    fun `null route returns false`() {
        assertFalse(shouldRunTutorialBoundsCleanup(currentRoute = null, dailyLogDone = true, trackerDone = true))
    }

    @Test
    fun `passphrase route returns false`() {
        assertFalse(
            shouldRunTutorialBoundsCleanup(
                currentRoute = NavRoute.Passphrase.route,
                dailyLogDone = true,
                trackerDone = true,
            ),
        )
    }

    // --- DailyLogHome route ---

    @Test
    fun `DailyLogHome with walkthrough not done returns false`() {
        assertFalse(
            shouldRunTutorialBoundsCleanup(
                currentRoute = NavRoute.DailyLogHome.route,
                dailyLogDone = false,
                trackerDone = false,
            ),
        )
    }

    @Test
    fun `DailyLogHome with DailyLog done but Tracker not done returns false`() {
        assertFalse(
            shouldRunTutorialBoundsCleanup(
                currentRoute = NavRoute.DailyLogHome.route,
                dailyLogDone = true,
                trackerDone = false,
            ),
        )
    }

    @Test
    fun `DailyLogHome with both walkthroughs done returns true`() {
        assertTrue(
            shouldRunTutorialBoundsCleanup(
                currentRoute = NavRoute.DailyLogHome.route,
                dailyLogDone = true,
                trackerDone = true,
            ),
        )
    }

    // --- Tracker route ---

    @Test
    fun `Tracker with walkthrough not done returns false`() {
        assertFalse(
            shouldRunTutorialBoundsCleanup(
                currentRoute = NavRoute.Tracker.route,
                dailyLogDone = true,
                trackerDone = false,
            ),
        )
    }

    @Test
    fun `Tracker with walkthrough done returns true`() {
        assertTrue(
            shouldRunTutorialBoundsCleanup(
                currentRoute = NavRoute.Tracker.route,
                dailyLogDone = true,
                trackerDone = true,
            ),
        )
    }

    // --- Other routes: always out of bounds ---

    @Test
    fun `Insights route returns true`() {
        assertTrue(
            shouldRunTutorialBoundsCleanup(
                currentRoute = NavRoute.Insights.route,
                dailyLogDone = false,
                trackerDone = false,
            ),
        )
    }

    @Test
    fun `Settings route returns true`() {
        assertTrue(
            shouldRunTutorialBoundsCleanup(
                currentRoute = NavRoute.Settings.route,
                dailyLogDone = false,
                trackerDone = false,
            ),
        )
    }

    @Test
    fun `DailyLog detail route returns true`() {
        assertTrue(
            shouldRunTutorialBoundsCleanup(
                currentRoute = NavRoute.DailyLog.route,
                dailyLogDone = false,
                trackerDone = false,
            ),
        )
    }
}
