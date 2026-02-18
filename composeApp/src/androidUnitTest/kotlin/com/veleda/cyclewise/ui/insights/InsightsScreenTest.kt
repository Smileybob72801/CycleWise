package com.veleda.cyclewise.ui.insights

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.veleda.cyclewise.domain.insights.CycleLengthAverage
import com.veleda.cyclewise.domain.insights.NextPeriodPrediction
import com.veleda.cyclewise.domain.insights.TopSymptomsInsight
import com.veleda.cyclewise.ui.theme.Dimensions
import com.veleda.cyclewise.ui.theme.LightCyclePhasePalette
import com.veleda.cyclewise.ui.theme.LocalCyclePhasePalette
import com.veleda.cyclewise.ui.theme.LocalDimensions
import kotlinx.datetime.LocalDate
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Robolectric-based Compose UI tests for [InsightsContent].
 *
 * Tests the internal [InsightsContent] composable directly, bypassing the
 * Koin-injected [InsightsScreen] wrapper.
 */
@RunWith(RobolectricTestRunner::class)
class InsightsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    /**
     * Helper that wraps content with the required composition locals.
     */
    private fun setInsightsContent(
        uiState: InsightsUiState,
        onRefresh: () -> Unit = {}
    ) {
        composeTestRule.setContent {
            CompositionLocalProvider(
                LocalDimensions provides Dimensions(),
                LocalCyclePhasePalette provides LightCyclePhasePalette,
            ) {
                MaterialTheme {
                    InsightsContent(
                        uiState = uiState,
                        onRefresh = onRefresh
                    )
                }
            }
        }
    }

    @Test
    fun emptyState_WHEN_noInsights_THEN_displaysEmptyHeading() {
        // GIVEN — no insights and not loading
        setInsightsContent(
            uiState = InsightsUiState(isLoading = false, insights = emptyList())
        )

        // THEN — the empty-state heading is displayed
        composeTestRule.onNodeWithText("No Insights Yet").assertIsDisplayed()
    }

    @Test
    fun loading_WHEN_isLoadingTrue_THEN_showsProgressIndicator() {
        // GIVEN — loading state
        setInsightsContent(
            uiState = InsightsUiState(isLoading = true, insights = emptyList())
        )

        // THEN — progress indicator is shown (no empty state heading)
        composeTestRule.onNodeWithText("No Insights Yet").assertDoesNotExist()
    }

    @Test
    fun cycleLengthCard_WHEN_rendered_THEN_showsLargeNumber() {
        // GIVEN — a CycleLengthAverage insight
        val insight = CycleLengthAverage(28.5)
        setInsightsContent(
            uiState = InsightsUiState(isLoading = false, insights = listOf(insight))
        )

        // THEN — the rounded number and "days" label are displayed
        composeTestRule.onNodeWithText("29").assertIsDisplayed()
        composeTestRule.onNodeWithText("days").assertIsDisplayed()
    }

    @Test
    fun predictionCard_WHEN_rendered_THEN_showsCountdown() {
        // GIVEN — a NextPeriodPrediction insight with 5 days until prediction
        val insight = NextPeriodPrediction(
            predictedDate = LocalDate(2026, 2, 21),
            daysUntilPrediction = 5
        )
        setInsightsContent(
            uiState = InsightsUiState(isLoading = false, insights = listOf(insight))
        )

        // THEN — countdown text is displayed
        composeTestRule.onNodeWithText("in 5 days").assertIsDisplayed()
    }

    @Test
    fun topSymptomsCard_WHEN_rendered_THEN_showsChips() {
        // GIVEN — a TopSymptomsInsight with symptom names
        val insight = TopSymptomsInsight(listOf("Headache", "Cramps"))
        setInsightsContent(
            uiState = InsightsUiState(isLoading = false, insights = listOf(insight))
        )

        // THEN — each symptom name is visible as a chip
        composeTestRule.onNodeWithText("Headache").assertIsDisplayed()
        composeTestRule.onNodeWithText("Cramps").assertIsDisplayed()
    }
}
