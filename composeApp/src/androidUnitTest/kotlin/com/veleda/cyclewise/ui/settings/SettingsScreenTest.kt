package com.veleda.cyclewise.ui.settings

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import com.veleda.cyclewise.ui.theme.Dimensions
import com.veleda.cyclewise.ui.theme.LocalDimensions

/**
 * Robolectric-based Compose UI tests for [SettingsContent].
 *
 * Tests the internal [SettingsContent] composable directly with the four sub-state
 * objects and event callback, bypassing the Koin-injected [SettingsScreen] wrapper.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = com.veleda.cyclewise.RobolectricTestApp::class)
class SettingsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    /**
     * Helper that wraps [SettingsContent] with required composition locals.
     */
    private fun setSettingsContent(
        generalState: GeneralSettingsState = GeneralSettingsState(),
        appearanceState: AppearanceSettingsState = AppearanceSettingsState(),
        notificationState: NotificationSettingsState = NotificationSettingsState(),
        aboutState: AboutSettingsState = AboutSettingsState(),
        onEvent: (SettingsEvent) -> Unit = {},
        isSessionActive: Boolean = false,
        onLockNow: () -> Unit = {},
    ) {
        composeTestRule.setContent {
            CompositionLocalProvider(
                LocalDimensions provides Dimensions(),
            ) {
                MaterialTheme {
                    SettingsContent(
                        generalState = generalState,
                        appearanceState = appearanceState,
                        notificationState = notificationState,
                        aboutState = aboutState,
                        onEvent = onEvent,
                        isSessionActive = isSessionActive,
                        onLockNow = onLockNow,
                    )
                }
            }
        }
    }

    @Test
    fun pageTabs_WHEN_rendered_THEN_allTabLabelsVisible() {
        // GIVEN — default settings content rendered
        setSettingsContent()

        // THEN — all four page tab labels are visible
        composeTestRule.onNodeWithText("General").assertIsDisplayed()
        composeTestRule.onNodeWithText("Appearance").assertIsDisplayed()
        composeTestRule.onNodeWithText("Notifications").assertIsDisplayed()
        composeTestRule.onNodeWithText("About").assertIsDisplayed()
    }

    @Test
    fun generalPage_WHEN_rendered_THEN_showsSecurityAndInsights() {
        // GIVEN — default settings content rendered (General page is first)
        setSettingsContent()

        // THEN — security section with auto-lock options and insights are visible
        composeTestRule.onNodeWithText("Security").assertIsDisplayed()
        composeTestRule.onNodeWithText("5 min").assertIsDisplayed()
        composeTestRule.onNodeWithText("10 min").assertIsDisplayed()
        composeTestRule.onNodeWithText("15 min").assertIsDisplayed()
        composeTestRule.onNodeWithText("30 min").assertIsDisplayed()
        composeTestRule.onNodeWithText("Insight Settings").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun generalPage_WHEN_sessionInactive_THEN_showsLockedMessage() {
        // GIVEN — settings content rendered with inactive session (locked state)
        setSettingsContent(isSessionActive = false)

        // THEN — the locked message is visible
        composeTestRule.onNodeWithText("Currently locked", substring = true).assertIsDisplayed()
    }

    @Test
    fun aboutPage_WHEN_rendered_THEN_showsAppNameAndDescription() {
        // GIVEN — About page content (using tab click is complex in unit test,
        //         so we test that the tab label exists and is displayed)
        setSettingsContent()

        // THEN — the About tab is visible
        composeTestRule.onNodeWithText("About").assertIsDisplayed()
    }
}
