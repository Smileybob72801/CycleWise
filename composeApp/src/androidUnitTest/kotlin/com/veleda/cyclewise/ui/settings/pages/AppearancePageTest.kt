package com.veleda.cyclewise.ui.settings.pages

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.veleda.cyclewise.RobolectricTestApp
import com.veleda.cyclewise.ui.settings.AppearanceSettingsState
import com.veleda.cyclewise.ui.settings.SettingsEvent
import com.veleda.cyclewise.ui.theme.Dimensions
import com.veleda.cyclewise.ui.theme.LightCyclePhasePalette
import com.veleda.cyclewise.ui.theme.LocalCyclePhasePalette
import com.veleda.cyclewise.ui.theme.LocalDimensions
import com.veleda.cyclewise.ui.theme.ThemeMode
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Robolectric-based Compose UI tests for [AppearancePage].
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = RobolectricTestApp::class)
class AppearancePageTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun setContent(
        state: AppearanceSettingsState = AppearanceSettingsState(),
        onEvent: (SettingsEvent) -> Unit = {},
    ) {
        composeTestRule.setContent {
            CompositionLocalProvider(
                LocalDimensions provides Dimensions(),
                LocalCyclePhasePalette provides LightCyclePhasePalette,
            ) {
                MaterialTheme {
                    AppearancePage(
                        state = state,
                        onEvent = onEvent,
                    )
                }
            }
        }
    }

    // region Theme section

    @Test
    fun themeSection_WHEN_rendered_THEN_titleDisplayed() {
        setContent()
        composeTestRule.onNodeWithText("Theme").assertIsDisplayed()
    }

    @Test
    fun themeButtons_WHEN_rendered_THEN_allThreeDisplayed() {
        setContent()
        composeTestRule.onAllNodesWithText("System")[0].assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Light")[0].assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Dark")[0].assertIsDisplayed()
    }

    @Test
    fun themeButton_WHEN_systemSelected_THEN_isSelected() {
        setContent(state = AppearanceSettingsState(themeMode = ThemeMode.SYSTEM))
        composeTestRule.onAllNodesWithText("System")[0].assertIsSelected()
    }

    @Test
    fun themeButton_WHEN_tapped_THEN_dispatchesEvent() {
        val events = mutableListOf<SettingsEvent>()
        setContent(onEvent = { events.add(it) })
        composeTestRule.onAllNodesWithText("Dark")[0].performClick()
        val changed = events.filterIsInstance<SettingsEvent.ThemeModeChanged>()
        assert(changed.any { it.mode == ThemeMode.DARK }) {
            "Expected ThemeModeChanged(DARK), got $events"
        }
    }

    // endregion

    // region Display section

    @Test
    fun displaySection_WHEN_rendered_THEN_titleDisplayed() {
        setContent()
        composeTestRule.onNodeWithText("Display").assertIsDisplayed()
    }

    @Test
    fun moodToggle_WHEN_rendered_THEN_labelDisplayed() {
        setContent()
        composeTestRule.onNodeWithText("Show Mood").assertIsDisplayed()
    }

    @Test
    fun energyToggle_WHEN_rendered_THEN_labelDisplayed() {
        setContent()
        composeTestRule.onNodeWithText("Show Energy").assertIsDisplayed()
    }

    @Test
    fun libidoToggle_WHEN_rendered_THEN_labelDisplayed() {
        setContent()
        composeTestRule.onNodeWithText("Show Libido").assertIsDisplayed()
    }

    // endregion

    // region Calendar Display section

    @Test
    fun calendarDisplaySection_WHEN_rendered_THEN_titleDisplayed() {
        setContent()
        composeTestRule.onNodeWithText("Calendar Display").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun phaseVisibility_WHEN_rendered_THEN_follicularLabelDisplayed() {
        setContent()
        composeTestRule.onNodeWithText("Follicular", substring = true).performScrollTo().assertIsDisplayed()
    }

    // endregion
}
