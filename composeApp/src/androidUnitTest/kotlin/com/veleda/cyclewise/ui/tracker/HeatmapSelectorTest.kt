package com.veleda.cyclewise.ui.tracker

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.veleda.cyclewise.domain.models.HeatmapMetric
import com.veleda.cyclewise.ui.theme.Dimensions
import com.veleda.cyclewise.ui.theme.LocalDimensions
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Robolectric-based Compose UI tests for [HeatmapSelector].
 */
@RunWith(RobolectricTestRunner::class)
class HeatmapSelectorTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun setContent(
        selectedMetric: HeatmapMetric? = null,
        onMetricSelected: (HeatmapMetric?) -> Unit = {},
    ) {
        composeTestRule.setContent {
            CompositionLocalProvider(
                LocalDimensions provides Dimensions(),
            ) {
                MaterialTheme {
                    HeatmapSelector(
                        selectedMetric = selectedMetric,
                        onMetricSelected = onMetricSelected,
                    )
                }
            }
        }
    }

    @Test
    fun `WHEN rendered THEN showsOffChip`() {
        setContent()
        composeTestRule.onNodeWithText("Off").assertIsDisplayed()
    }

    @Test
    fun `WHEN rendered THEN showsAllMetricChips`() {
        setContent()
        composeTestRule.onNodeWithText("Mood").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Energy").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Libido").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Water").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Symptom Severity").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Flow").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Medications").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `WHEN offClicked THEN emitsNull`() {
        var selected: HeatmapMetric? = HeatmapMetric.Mood
        setContent(
            selectedMetric = HeatmapMetric.Mood,
            onMetricSelected = { selected = it },
        )
        composeTestRule.onNodeWithText("Off").performClick()
        assertNull(selected)
    }

    @Test
    fun `WHEN metricClicked THEN emitsMetric`() {
        var selected: HeatmapMetric? = null
        setContent(
            selectedMetric = null,
            onMetricSelected = { selected = it },
        )
        composeTestRule.onNodeWithText("Energy").performClick()
        assertEquals(HeatmapMetric.Energy, selected)
    }

    @Test
    fun `WHEN sameMetricClicked THEN togglesOff`() {
        var selected: HeatmapMetric? = HeatmapMetric.Mood
        setContent(
            selectedMetric = HeatmapMetric.Mood,
            onMetricSelected = { selected = it },
        )
        composeTestRule.onNodeWithText("Mood").performClick()
        assertNull(selected)
    }
}
