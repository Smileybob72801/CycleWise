package com.veleda.cyclewise.ui.settings

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import com.veleda.cyclewise.settings.AppSettings
import com.veleda.cyclewise.ui.theme.Dimensions
import com.veleda.cyclewise.ui.theme.LocalDimensions
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Robolectric-based Compose UI tests for [PhaseColorSettings].
 *
 * Tests the composable directly with a real [AppSettings] backed by
 * Robolectric's in-memory DataStore.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = com.veleda.cyclewise.RobolectricTestApp::class)
class PhaseColorSettingsTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val appSettings = AppSettings(ApplicationProvider.getApplicationContext())

    /**
     * Helper that wraps [PhaseColorSettings] with required composition locals.
     */
    private fun setPhaseColorContent() {
        composeTestRule.setContent {
            CompositionLocalProvider(
                LocalDimensions provides Dimensions(),
            ) {
                MaterialTheme {
                    PhaseColorSettings(appSettings)
                }
            }
        }
    }

    @Test
    fun phaseColorSettings_WHEN_rendered_THEN_showsFourPhaseLabels() {
        // GIVEN — phase color settings rendered
        setPhaseColorContent()

        // THEN — all four phase labels are visible
        composeTestRule.onNodeWithText("Period").assertIsDisplayed()
        composeTestRule.onNodeWithText("Follicular").assertIsDisplayed()
        composeTestRule.onNodeWithText("Ovulation").assertIsDisplayed()
        composeTestRule.onNodeWithText("Luteal").assertIsDisplayed()
    }

    @Test
    fun presetGrid_WHEN_rendered_THEN_showsPresetSwatches() {
        // GIVEN — phase color settings rendered
        setPhaseColorContent()

        // THEN — at least one preset swatch is accessible by its content description
        composeTestRule
            .onAllNodesWithContentDescription("Select color F48FB1")
            .onFirst()
            .assertIsDisplayed()
    }
}
