package com.veleda.cyclewise.ui.tracker

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.veleda.cyclewise.RobolectricTestApp
import com.veleda.cyclewise.domain.models.CyclePhase
import com.veleda.cyclewise.ui.theme.CyclePhasePalette
import com.veleda.cyclewise.ui.theme.Dimensions
import com.veleda.cyclewise.ui.theme.LocalDimensions
import com.veleda.cyclewise.ui.theme.buildCyclePhasePalette
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Robolectric-based Compose UI tests for [PhaseLegend].
 *
 * Verifies that all visible phase chips render at various screen widths,
 * including narrow widths (320 dp–360 dp) where the previous [Row]-based
 * layout caused vertical text wrapping.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = RobolectricTestApp::class)
class PhaseLegendTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val palette: CyclePhasePalette = buildCyclePhasePalette(darkTheme = false)

    private fun setContent(
        width: Dp,
        phaseVisible: Map<CyclePhase, Boolean> = emptyMap(),
        heatmapActive: Boolean = false,
    ) {
        composeTestRule.setContent {
            CompositionLocalProvider(
                LocalDimensions provides Dimensions(),
            ) {
                MaterialTheme {
                    Box(modifier = Modifier.requiredWidth(width)) {
                        PhaseLegend(
                            palette = palette,
                            phaseVisible = phaseVisible,
                            heatmapActive = heatmapActive,
                        )
                    }
                }
            }
        }
    }

    // --- All four labels at various widths ---

    @Test
    fun `GIVEN normalWidth WHEN allPhasesVisible THEN allFourLabelsDisplayed`() {
        // GIVEN a normal-width container (411 dp)
        // WHEN all phases are visible (default)
        setContent(width = 411.dp)

        // THEN all four phase labels are displayed
        composeTestRule.onNodeWithText("Period").assertIsDisplayed()
        composeTestRule.onNodeWithText("Follicular").assertIsDisplayed()
        composeTestRule.onNodeWithText("Ovulation").assertIsDisplayed()
        composeTestRule.onNodeWithText("Luteal").assertIsDisplayed()
    }

    @Test
    fun `GIVEN narrowWidth320 WHEN allPhasesVisible THEN allFourLabelsDisplayed`() {
        // GIVEN the narrowest target width (320 dp)
        // WHEN all phases are visible (default)
        setContent(width = 320.dp)

        // THEN all four phase labels are displayed (wrapping to second row as needed)
        composeTestRule.onNodeWithText("Period").assertIsDisplayed()
        composeTestRule.onNodeWithText("Follicular").assertIsDisplayed()
        composeTestRule.onNodeWithText("Ovulation").assertIsDisplayed()
        composeTestRule.onNodeWithText("Luteal").assertIsDisplayed()
    }

    @Test
    fun `GIVEN narrowWidth360 WHEN allPhasesVisible THEN allFourLabelsDisplayed`() {
        // GIVEN a mid-range narrow width (360 dp)
        // WHEN all phases are visible (default)
        setContent(width = 360.dp)

        // THEN all four phase labels are displayed
        composeTestRule.onNodeWithText("Period").assertIsDisplayed()
        composeTestRule.onNodeWithText("Follicular").assertIsDisplayed()
        composeTestRule.onNodeWithText("Ovulation").assertIsDisplayed()
        composeTestRule.onNodeWithText("Luteal").assertIsDisplayed()
    }

    // --- Visibility filtering ---

    @Test
    fun `GIVEN onlyPeriodVisible WHEN rendered THEN onlyPeriodLabelDisplayed`() {
        // GIVEN all non-period phases are hidden
        setContent(
            width = 411.dp,
            phaseVisible = mapOf(
                CyclePhase.FOLLICULAR to false,
                CyclePhase.OVULATION to false,
                CyclePhase.LUTEAL to false,
            ),
        )

        // THEN only Period is displayed
        composeTestRule.onNodeWithText("Period").assertIsDisplayed()
        composeTestRule.onNodeWithText("Follicular").assertDoesNotExist()
        composeTestRule.onNodeWithText("Ovulation").assertDoesNotExist()
        composeTestRule.onNodeWithText("Luteal").assertDoesNotExist()
    }

    @Test
    fun `GIVEN onePhaseHidden WHEN narrowWidth320 THEN threeChipsDisplayed`() {
        // GIVEN ovulation is hidden at 320 dp
        setContent(
            width = 320.dp,
            phaseVisible = mapOf(CyclePhase.OVULATION to false),
        )

        // THEN three phase labels are displayed, ovulation is absent
        composeTestRule.onNodeWithText("Period").assertIsDisplayed()
        composeTestRule.onNodeWithText("Follicular").assertIsDisplayed()
        composeTestRule.onNodeWithText("Luteal").assertIsDisplayed()
        composeTestRule.onNodeWithText("Ovulation").assertDoesNotExist()
    }

    // --- Heatmap-active outlined variant ---

    @Test
    fun `GIVEN heatmapActive true THEN allFourLabelsStillDisplayed`() {
        // GIVEN heatmap is active — swatches should render as outlined rings
        setContent(width = 411.dp, heatmapActive = true)

        // THEN all four phase labels are still displayed
        composeTestRule.onNodeWithText("Period").assertIsDisplayed()
        composeTestRule.onNodeWithText("Follicular").assertIsDisplayed()
        composeTestRule.onNodeWithText("Ovulation").assertIsDisplayed()
        composeTestRule.onNodeWithText("Luteal").assertIsDisplayed()
    }

    @Test
    fun `GIVEN heatmapActive false THEN allFourLabelsDisplayed`() {
        // GIVEN heatmap is inactive — normal filled swatches
        setContent(width = 411.dp, heatmapActive = false)

        // THEN all four phase labels are displayed
        composeTestRule.onNodeWithText("Period").assertIsDisplayed()
        composeTestRule.onNodeWithText("Follicular").assertIsDisplayed()
        composeTestRule.onNodeWithText("Ovulation").assertIsDisplayed()
        composeTestRule.onNodeWithText("Luteal").assertIsDisplayed()
    }
}
