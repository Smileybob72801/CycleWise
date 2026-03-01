package com.veleda.cyclewise.ui.settings.pages

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.veleda.cyclewise.RobolectricTestApp
import com.veleda.cyclewise.ui.settings.NotificationSettingsState
import com.veleda.cyclewise.ui.settings.SettingsEvent
import com.veleda.cyclewise.ui.theme.Dimensions
import com.veleda.cyclewise.ui.theme.LocalDimensions
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Robolectric-based Compose UI tests for [NotificationsPage].
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = RobolectricTestApp::class)
class NotificationsPageTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun setContent(
        state: NotificationSettingsState = NotificationSettingsState(),
        onEvent: (SettingsEvent) -> Unit = {},
    ) {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalDimensions provides Dimensions()) {
                MaterialTheme {
                    NotificationsPage(
                        state = state,
                        onEvent = onEvent,
                    )
                }
            }
        }
    }

    // region Section visibility

    @Test
    fun notificationsSection_WHEN_rendered_THEN_titleDisplayed() {
        setContent()
        composeTestRule.onNodeWithText("Notifications").assertIsDisplayed()
    }

    @Test
    fun periodReminder_WHEN_rendered_THEN_sectionDisplayed() {
        setContent()
        composeTestRule.onNodeWithText("Period Prediction").assertIsDisplayed()
    }

    @Test
    fun medicationReminder_WHEN_rendered_THEN_sectionDisplayed() {
        setContent()
        composeTestRule.onNodeWithText("Daily Medication").assertIsDisplayed()
    }

    @Test
    fun hydrationReminder_WHEN_rendered_THEN_sectionDisplayed() {
        setContent()
        composeTestRule.onNodeWithText("Hydration").assertIsDisplayed()
    }

    // endregion

    // region Period reminder sub-sections

    @Test
    fun periodSubSection_WHEN_enabled_THEN_daysBeforeDisplayed() {
        setContent(
            state = NotificationSettingsState(
                periodReminderEnabled = true,
                periodPrivacyAccepted = true,
            ),
        )
        // "Days before: 2" should appear when enabled
        composeTestRule.onNodeWithText("Days before", substring = true).assertIsDisplayed()
    }

    @Test
    fun periodSubSection_WHEN_disabled_THEN_daysBeforeNotDisplayed() {
        setContent(state = NotificationSettingsState(periodReminderEnabled = false))
        composeTestRule.onNodeWithText("Days before", substring = true).assertDoesNotExist()
    }

    // endregion

    // region Medication reminder sub-sections

    @Test
    fun medicationSubSection_WHEN_enabled_THEN_timeDisplayed() {
        setContent(state = NotificationSettingsState(medicationReminderEnabled = true))
        composeTestRule.onNodeWithText("Reminder time", substring = true).assertIsDisplayed()
    }

    @Test
    fun medicationSubSection_WHEN_disabled_THEN_timeNotDisplayed() {
        setContent(state = NotificationSettingsState(medicationReminderEnabled = false))
        composeTestRule.onNodeWithText("Reminder time", substring = true).assertDoesNotExist()
    }

    // endregion

    // region Hydration reminder sub-sections

    @Test
    fun hydrationSubSection_WHEN_enabled_THEN_goalDisplayed() {
        setContent(state = NotificationSettingsState(hydrationReminderEnabled = true))
        composeTestRule.onNodeWithText("Daily goal", substring = true).assertIsDisplayed()
    }

    @Test
    fun hydrationSubSection_WHEN_disabled_THEN_goalNotDisplayed() {
        setContent(state = NotificationSettingsState(hydrationReminderEnabled = false))
        composeTestRule.onNodeWithText("Daily goal", substring = true).assertDoesNotExist()
    }

    // endregion
}
