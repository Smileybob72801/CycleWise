package com.veleda.cyclewise.ui.nav

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Robolectric-based Compose UI tests for [BottomNavBar].
 *
 * Uses a minimal [NavHost] with empty composable destinations so that
 * navigation state updates are observable through the bottom bar.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = com.veleda.cyclewise.RobolectricTestApp::class)
class BottomNavBarTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    /**
     * Sets up a minimal scaffold with [BottomNavBar] wired to a [NavHost]
     * whose start destination is [NavRoute.Tracker].
     */
    private fun setBottomNavContent(enabled: Boolean = true) {
        composeTestRule.setContent {
            MaterialTheme {
                val navController = rememberNavController()
                NavHost(
                    navController = navController,
                    startDestination = NavRoute.Tracker.route,
                ) {
                    composable(NavRoute.Tracker.route) {}
                    composable(NavRoute.Insights.route) {}
                    composable(NavRoute.Settings.route) {}
                }
                BottomNavBar(navController, enabled = enabled)
            }
        }
    }

    @Test
    fun allThreeTabs_WHEN_rendered_THEN_labelsAreDisplayed() {
        // GIVEN — the bottom nav bar is rendered
        setBottomNavContent()

        // THEN — all three tab labels are visible
        composeTestRule.onNodeWithText("Tracker").assertIsDisplayed()
        composeTestRule.onNodeWithText("Insights").assertIsDisplayed()
        composeTestRule.onNodeWithText("Settings").assertIsDisplayed()
    }

    @Test
    fun trackerTab_WHEN_startDestination_THEN_isSelected() {
        // GIVEN — the start destination is Tracker
        setBottomNavContent()

        // THEN — Tracker tab is selected, others are not
        composeTestRule.onNodeWithTag("bottom-nav-tracker").assertIsSelected()
        composeTestRule.onNodeWithTag("bottom-nav-insights").assertIsNotSelected()
        composeTestRule.onNodeWithTag("bottom-nav-settings").assertIsNotSelected()
    }

    @Test
    fun insightsTab_WHEN_tapped_THEN_becomesSelected() {
        // GIVEN — the bottom nav is rendered with Tracker selected
        setBottomNavContent()

        // WHEN — Insights tab is tapped
        composeTestRule.onNodeWithTag("bottom-nav-insights").performClick()
        composeTestRule.waitForIdle()

        // THEN — Insights is now selected
        composeTestRule.onNodeWithTag("bottom-nav-insights").assertIsSelected()
    }

    @Test
    fun settingsTab_WHEN_tapped_THEN_becomesSelected() {
        // GIVEN — the bottom nav is rendered with Tracker selected
        setBottomNavContent()

        // WHEN — Settings tab is tapped
        composeTestRule.onNodeWithTag("bottom-nav-settings").performClick()
        composeTestRule.waitForIdle()

        // THEN — Settings is now selected
        composeTestRule.onNodeWithTag("bottom-nav-settings").assertIsSelected()
    }

    @Test
    fun passphraseRoute_WHEN_bottomNavRendered_THEN_notPresentAsTab() {
        // GIVEN — the bottom nav is rendered
        setBottomNavContent()

        // THEN — no tab for "Pass Phrase" exists
        composeTestRule.onNodeWithText("Pass Phrase").assertDoesNotExist()
    }

    @Test
    fun insightsTab_WHEN_disabledAndTapped_THEN_trackerRemainsSelected() {
        // GIVEN — the bottom nav is rendered with enabled = false
        setBottomNavContent(enabled = false)

        // WHEN — Insights tab is tapped while disabled
        composeTestRule.onNodeWithTag("bottom-nav-insights").performClick()
        composeTestRule.waitForIdle()

        // THEN — Tracker is still selected (navigation did not happen)
        composeTestRule.onNodeWithTag("bottom-nav-tracker").assertIsSelected()
        composeTestRule.onNodeWithTag("bottom-nav-insights").assertIsNotSelected()
    }

    @Test
    fun allTabs_WHEN_disabled_THEN_navItemsAreNotEnabled() {
        // GIVEN — the bottom nav is rendered with enabled = false
        setBottomNavContent(enabled = false)

        // THEN — all navigation items report as not enabled
        composeTestRule.onNodeWithTag("bottom-nav-tracker").assertIsNotEnabled()
        composeTestRule.onNodeWithTag("bottom-nav-insights").assertIsNotEnabled()
        composeTestRule.onNodeWithTag("bottom-nav-settings").assertIsNotEnabled()
    }

    @Test
    fun allTabs_WHEN_enabled_THEN_navItemsAreEnabled() {
        // GIVEN — the bottom nav is rendered with default enabled = true
        setBottomNavContent()

        // THEN — all navigation items report as enabled
        composeTestRule.onNodeWithTag("bottom-nav-tracker").assertIsEnabled()
        composeTestRule.onNodeWithTag("bottom-nav-insights").assertIsEnabled()
        composeTestRule.onNodeWithTag("bottom-nav-settings").assertIsEnabled()
    }
}
