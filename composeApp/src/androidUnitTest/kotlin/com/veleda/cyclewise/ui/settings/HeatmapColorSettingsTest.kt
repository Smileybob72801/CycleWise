package com.veleda.cyclewise.ui.settings

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.veleda.cyclewise.ui.theme.Dimensions
import com.veleda.cyclewise.ui.theme.LocalDimensions
import com.veleda.cyclewise.ui.tracker.HeatmapMetricColors
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Robolectric-based Compose UI tests for [HeatmapColorSettings].
 *
 * Tests the composable directly with state values and no-op / capturing callbacks.
 * The composable is wrapped in a vertically scrollable container so that
 * [performScrollTo] works for nodes that may be below the fold.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = com.veleda.cyclewise.RobolectricTestApp::class)
class HeatmapColorSettingsTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    /**
     * Helper that wraps [HeatmapColorSettings] inside a scrollable Column with
     * required composition locals and default hex values.
     */
    private fun setHeatmapColorContent(
        onColorChanged: (String, String) -> Unit = { _, _ -> },
        onResetDefaults: () -> Unit = {},
        showTitle: Boolean = false,
    ) {
        composeTestRule.setContent {
            CompositionLocalProvider(
                LocalDimensions provides Dimensions(),
            ) {
                MaterialTheme {
                    androidx.compose.foundation.layout.Column(
                        modifier = Modifier.verticalScroll(rememberScrollState()),
                    ) {
                        HeatmapColorSettings(
                            moodHex = HeatmapMetricColors.DEFAULT_MOOD_HEX,
                            energyHex = HeatmapMetricColors.DEFAULT_ENERGY_HEX,
                            libidoHex = HeatmapMetricColors.DEFAULT_LIBIDO_HEX,
                            waterIntakeHex = HeatmapMetricColors.DEFAULT_WATER_INTAKE_HEX,
                            symptomSeverityHex = HeatmapMetricColors.DEFAULT_SYMPTOM_SEVERITY_HEX,
                            flowIntensityHex = HeatmapMetricColors.DEFAULT_FLOW_INTENSITY_HEX,
                            medicationCountHex = HeatmapMetricColors.DEFAULT_MEDICATION_COUNT_HEX,
                            onColorChanged = onColorChanged,
                            onResetDefaults = onResetDefaults,
                            showTitle = showTitle,
                        )
                    }
                }
            }
        }
    }

    @Test
    fun heatmapColorSettings_WHEN_rendered_THEN_showsAllMetricLabels() {
        // GIVEN — heatmap color settings rendered
        setHeatmapColorContent()

        // THEN — all seven metric labels are visible
        composeTestRule.onNodeWithText("Mood").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Energy").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Libido").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Water").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Symptom Severity").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Flow").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Medications").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun heatmapColorSettings_WHEN_rendered_THEN_showsResetButton() {
        // GIVEN — heatmap color settings rendered
        setHeatmapColorContent()

        // THEN — the reset button is visible
        composeTestRule.onNodeWithText("Reset to Defaults")
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun resetButton_WHEN_clicked_THEN_triggersCallback() {
        // GIVEN — a capturing callback
        var resetCalled = false
        setHeatmapColorContent(onResetDefaults = { resetCalled = true })

        // WHEN — user taps "Reset to Defaults"
        composeTestRule.onNodeWithText("Reset to Defaults")
            .performScrollTo()
            .performClick()

        // THEN — the callback was invoked
        assertEquals(true, resetCalled)
    }

    @Test
    fun heatmapColorSettings_WHEN_renderedWithTitle_THEN_showsTitleText() {
        // GIVEN — heatmap color settings rendered with title enabled
        setHeatmapColorContent(showTitle = true)

        // THEN — the title text is displayed
        composeTestRule.onNodeWithText("Heatmap Colors").assertIsDisplayed()
    }
}
