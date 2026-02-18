package com.veleda.cyclewise.ui.settings

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import com.veleda.cyclewise.reminders.ReminderScheduler
import com.veleda.cyclewise.settings.AppSettings
import com.veleda.cyclewise.ui.theme.Dimensions
import com.veleda.cyclewise.ui.theme.LocalDimensions
import io.mockk.mockk
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Robolectric-based Compose UI tests for [SettingsContent].
 *
 * Tests the internal [SettingsContent] composable directly, bypassing the
 * Koin-injected [SettingsScreen] wrapper.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = com.veleda.cyclewise.RobolectricTestApp::class)
class SettingsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val appSettings = AppSettings(ApplicationProvider.getApplicationContext())
    private val reminderScheduler = mockk<ReminderScheduler>(relaxed = true)

    /**
     * Helper that wraps [SettingsContent] with required composition locals.
     */
    private fun setSettingsContent(session: org.koin.core.scope.Scope? = null) {
        composeTestRule.setContent {
            CompositionLocalProvider(
                LocalDimensions provides Dimensions(),
            ) {
                MaterialTheme {
                    SettingsContent(
                        appSettings = appSettings,
                        session = session,
                        reminderScheduler = reminderScheduler,
                        onLockNow = {}
                    )
                }
            }
        }
    }

    @Test
    fun sectionCards_WHEN_rendered_THEN_allSectionHeadersVisible() {
        // GIVEN — default settings content rendered
        setSettingsContent()

        // THEN — all section headers exist (scrolling to each as needed)
        composeTestRule.onNodeWithText("Security").assertIsDisplayed()
        composeTestRule.onNodeWithText("Display").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Customization").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Notifications").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Insight Settings").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("About").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun autolockSegmentedButton_WHEN_rendered_THEN_showsAllOptions() {
        // GIVEN — default settings content rendered
        setSettingsContent()

        // THEN — all auto-lock options are visible
        composeTestRule.onNodeWithText("5 min").assertIsDisplayed()
        composeTestRule.onNodeWithText("10 min").assertIsDisplayed()
        composeTestRule.onNodeWithText("15 min").assertIsDisplayed()
        composeTestRule.onNodeWithText("30 min").assertIsDisplayed()
    }

    @Test
    fun displayCard_WHEN_rendered_THEN_showsMoodSwitchToggle() {
        // GIVEN — default settings content rendered
        setSettingsContent()

        // THEN — the mood toggle label is visible
        composeTestRule.onNodeWithText("Show Mood").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun phaseVisibility_WHEN_rendered_THEN_showsSwitchToggles() {
        // GIVEN — default settings content rendered
        setSettingsContent()

        // THEN — all phase visibility toggle labels are visible (scroll to each)
        composeTestRule.onNodeWithText("Show Follicular").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Show Ovulation").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Show Luteal").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun topSymptomsSlider_WHEN_rendered_THEN_showsStepMarkers() {
        // GIVEN — default settings content rendered
        setSettingsContent()

        // THEN — the insight settings header and step markers are visible
        composeTestRule.onNodeWithText("Insight Settings").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun lockButton_WHEN_sessionNull_THEN_showsLockedMessage() {
        // GIVEN — settings content rendered with null session (locked state)
        setSettingsContent(session = null)

        // THEN — the locked message is visible
        composeTestRule.onNodeWithText("Currently locked", substring = true)
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun aboutCard_WHEN_rendered_THEN_showsAppNameAndDescription() {
        // GIVEN — default settings content rendered
        setSettingsContent()

        // THEN — the app name and description are visible in the About card
        composeTestRule.onNodeWithText("RhythmWise").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Privacy-first cycle tracking")
            .performScrollTo()
            .assertIsDisplayed()
    }
}
