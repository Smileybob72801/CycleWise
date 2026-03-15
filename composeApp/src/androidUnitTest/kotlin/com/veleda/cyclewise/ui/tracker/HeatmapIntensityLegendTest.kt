package com.veleda.cyclewise.ui.tracker

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.veleda.cyclewise.RobolectricTestApp
import com.veleda.cyclewise.ui.theme.Dimensions
import com.veleda.cyclewise.ui.theme.LocalDimensions
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Robolectric-based integration tests for [HeatmapIntensityLegend].
 *
 * Validates that the gradient bar renders with "Low" and "High" labels
 * and that the composable's test tag is present.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = RobolectricTestApp::class)
class HeatmapIntensityLegendTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val testColor = Color(0xFF1976D2)

    private fun setContent(metricColor: Color = testColor) {
        composeTestRule.setContent {
            CompositionLocalProvider(
                LocalDimensions provides Dimensions(),
            ) {
                MaterialTheme {
                    HeatmapIntensityLegend(metricColor = metricColor)
                }
            }
        }
    }

    @Test
    fun `GIVEN metricColor THEN rendersLowAndHighLabels`() {
        // GIVEN a metric color
        setContent()

        // THEN "Low" and "High" labels are displayed
        composeTestRule.onNodeWithText("Low").assertIsDisplayed()
        composeTestRule.onNodeWithText("High").assertIsDisplayed()
    }

    @Test
    fun `GIVEN metricColor THEN rendersGradientBar`() {
        // GIVEN a metric color
        setContent()

        // THEN the gradient bar test tag is present
        composeTestRule.onNodeWithTag("heatmap-intensity-legend").assertIsDisplayed()
    }
}
