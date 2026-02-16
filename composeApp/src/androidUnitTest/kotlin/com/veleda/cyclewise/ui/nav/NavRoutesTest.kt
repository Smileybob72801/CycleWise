package com.veleda.cyclewise.ui.nav

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Settings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Unit tests verifying [NavRoute] icon metadata and companion-object membership.
 *
 * Uses Robolectric so that [androidx.compose.ui.graphics.vector.ImageVector] references
 * resolve without a full Android device.
 */
@RunWith(RobolectricTestRunner::class)
class NavRoutesTest {

    // ── Tracker ────────────────────────────────────────────────────────

    @Test
    fun tracker_WHEN_selected_THEN_iconIsFilledCalendarMonth() {
        // GIVEN — the Tracker route
        // WHEN — selectedIcon is accessed
        // THEN — it equals the filled CalendarMonth icon
        assertEquals(Icons.Filled.CalendarMonth, NavRoute.Tracker.selectedIcon)
    }

    @Test
    fun tracker_WHEN_unselected_THEN_iconIsOutlinedCalendarMonth() {
        // GIVEN — the Tracker route
        // WHEN — unselectedIcon is accessed
        // THEN — it equals the outlined CalendarMonth icon
        assertEquals(Icons.Outlined.CalendarMonth, NavRoute.Tracker.unselectedIcon)
    }

    // ── Insights ───────────────────────────────────────────────────────

    @Test
    fun insights_WHEN_selected_THEN_iconIsFilledInsights() {
        // GIVEN — the Insights route
        // WHEN — selectedIcon is accessed
        // THEN — it equals the filled Insights icon
        assertEquals(Icons.Filled.Insights, NavRoute.Insights.selectedIcon)
    }

    @Test
    fun insights_WHEN_unselected_THEN_iconIsOutlinedInsights() {
        // GIVEN — the Insights route
        // WHEN — unselectedIcon is accessed
        // THEN — it equals the outlined Insights icon
        assertEquals(Icons.Outlined.Insights, NavRoute.Insights.unselectedIcon)
    }

    // ── Settings ───────────────────────────────────────────────────────

    @Test
    fun settings_WHEN_selected_THEN_iconIsFilledSettings() {
        // GIVEN — the Settings route
        // WHEN — selectedIcon is accessed
        // THEN — it equals the filled Settings icon
        assertEquals(Icons.Filled.Settings, NavRoute.Settings.selectedIcon)
    }

    @Test
    fun settings_WHEN_unselected_THEN_iconIsOutlinedSettings() {
        // GIVEN — the Settings route
        // WHEN — unselectedIcon is accessed
        // THEN — it equals the outlined Settings icon
        assertEquals(Icons.Outlined.Settings, NavRoute.Settings.unselectedIcon)
    }

    // ── Non-bottom-bar routes ──────────────────────────────────────────

    @Test
    fun passphrase_WHEN_accessed_THEN_iconsAreNull() {
        // GIVEN — the Passphrase route (not a bottom-nav tab)
        // WHEN — icons are accessed
        // THEN — both are null
        assertNull(NavRoute.Passphrase.selectedIcon)
        assertNull(NavRoute.Passphrase.unselectedIcon)
    }

    @Test
    fun dailyLog_WHEN_accessed_THEN_iconsAreNull() {
        // GIVEN — the DailyLog route (not a bottom-nav tab)
        // WHEN — icons are accessed
        // THEN — both are null
        assertNull(NavRoute.DailyLog.selectedIcon)
        assertNull(NavRoute.DailyLog.unselectedIcon)
    }

    // ── Companion.all ──────────────────────────────────────────────────

    @Test
    fun all_WHEN_accessed_THEN_containsExactlyThreeBottomNavRoutes() {
        // GIVEN — the companion-object list of bottom-nav routes
        // WHEN — size and contents are inspected
        // THEN — exactly Tracker, Insights, Settings are present
        assertEquals(
            listOf(NavRoute.Tracker, NavRoute.Insights, NavRoute.Settings),
            NavRoute.all,
        )
    }
}
