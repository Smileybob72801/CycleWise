package com.veleda.cyclewise.ui.log.pages

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.veleda.cyclewise.RobolectricTestApp
import com.veleda.cyclewise.ui.theme.Dimensions
import com.veleda.cyclewise.ui.theme.LocalDimensions
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Robolectric-based Compose UI tests for [WellnessPage].
 *
 * Tests the internal [WellnessPage] composable directly, passing
 * callback lambdas to capture user interactions.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = RobolectricTestApp::class)
class WellnessPageTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun setContent(
        moodScore: Int? = null,
        energyLevel: Int? = null,
        libidoScore: Int? = null,
        waterCups: Int = 0,
        onMoodChanged: (Int) -> Unit = {},
        onEnergyChanged: (Int) -> Unit = {},
        onLibidoChanged: (Int) -> Unit = {},
        onWaterIncrement: () -> Unit = {},
        onWaterDecrement: () -> Unit = {},
        onShowEducationalSheet: (String) -> Unit = {},
    ) {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalDimensions provides Dimensions()) {
                MaterialTheme {
                    WellnessPage(
                        moodScore = moodScore,
                        energyLevel = energyLevel,
                        libidoScore = libidoScore,
                        waterCups = waterCups,
                        onMoodChanged = onMoodChanged,
                        onEnergyChanged = onEnergyChanged,
                        onLibidoChanged = onLibidoChanged,
                        onWaterIncrement = onWaterIncrement,
                        onWaterDecrement = onWaterDecrement,
                        onShowEducationalSheet = onShowEducationalSheet,
                    )
                }
            }
        }
    }

    // region Empty state

    @Test
    fun emptyState_WHEN_allValuesUnset_THEN_promptIsDisplayed() {
        // Given / When — all defaults (null/0)
        setContent()

        // Then
        composeTestRule.onNodeWithText("Start by rating", substring = true, ignoreCase = true)
            .assertIsDisplayed()
    }

    @Test
    fun emptyState_WHEN_moodIsSet_THEN_promptIsHidden() {
        // Given / When
        setContent(moodScore = 3)

        // Then
        composeTestRule.onNodeWithText("Start by rating", substring = true, ignoreCase = true)
            .assertDoesNotExist()
    }

    @Test
    fun emptyState_WHEN_waterIsPositive_THEN_promptIsHidden() {
        // Given / When
        setContent(waterCups = 1)

        // Then
        composeTestRule.onNodeWithText("Start by rating", substring = true, ignoreCase = true)
            .assertDoesNotExist()
    }

    // endregion

    // region Section titles

    @Test
    fun moodSection_WHEN_rendered_THEN_titleIsDisplayed() {
        // Given / When
        setContent()

        // Then
        composeTestRule.onNodeWithText("Mood", substring = true).assertIsDisplayed()
    }

    @Test
    fun energySection_WHEN_rendered_THEN_titleIsDisplayed() {
        // Given / When
        setContent()

        // Then
        composeTestRule.onNodeWithText("Energy", substring = true).assertIsDisplayed()
    }

    @Test
    fun libidoSection_WHEN_rendered_THEN_titleIsDisplayed() {
        // Given / When
        setContent()

        // Then
        composeTestRule.onNodeWithText("Libido", substring = true).assertIsDisplayed()
    }

    @Test
    fun waterSection_WHEN_rendered_THEN_titleIsDisplayed() {
        // Given / When
        setContent()

        // Then — multiple nodes may contain "Water"; verify at least one is displayed
        composeTestRule.onAllNodes(
            androidx.compose.ui.test.hasText("Water", substring = true, ignoreCase = true),
        )[0].assertIsDisplayed()
    }

    // endregion

    // region Mood star selector callbacks

    @Test
    fun moodSelector_WHEN_starTapped_THEN_invokesCallbackWithScore() {
        // Given
        var captured: Int? = null
        setContent(onMoodChanged = { captured = it })

        // When — tap the first mood star (content description "Mood score 1")
        composeTestRule.onAllNodesWithContentDescription("Mood score 1")[0]
            .performClick()

        // Then
        assert(captured == 1) { "Expected mood score 1, got $captured" }
    }

    // endregion

    // region Energy selector callbacks

    @Test
    fun energySelector_WHEN_starTapped_THEN_invokesCallback() {
        // Given
        var captured: Int? = null
        setContent(onEnergyChanged = { captured = it })

        // When — content description "Energy score 1"
        composeTestRule.onAllNodesWithContentDescription("Energy score 1")[0]
            .performClick()

        // Then
        assert(captured == 1) { "Expected energy level 1, got $captured" }
    }

    // endregion

    // region Water callbacks

    @Test
    fun waterSection_WHEN_incrementTapped_THEN_invokesCallback() {
        // Given
        var incremented = false
        setContent(onWaterIncrement = { incremented = true })

        // When — water section is at the bottom, needs scrolling
        composeTestRule.onNodeWithTag("water-increment", useUnmergedTree = true)
            .performScrollTo()
            .performClick()

        // Then
        assert(incremented) { "onWaterIncrement was not invoked" }
    }

    @Test
    fun waterSection_WHEN_decrementTapped_THEN_invokesCallback() {
        // Given
        var decremented = false
        setContent(waterCups = 1, onWaterDecrement = { decremented = true })

        // When — water section is at the bottom, needs scrolling
        composeTestRule.onNodeWithTag("water-decrement", useUnmergedTree = true)
            .performScrollTo()
            .performClick()

        // Then
        assert(decremented) { "onWaterDecrement was not invoked" }
    }

    // endregion

    // region Info buttons

    @Test
    fun moodInfoButton_WHEN_tapped_THEN_invokesEducationalSheet() {
        // Given
        var capturedTag: String? = null
        setContent(onShowEducationalSheet = { capturedTag = it })

        // When — info button has content description "Learn more about Mood"
        composeTestRule.onAllNodesWithContentDescription("Learn more about Mood")[0]
            .performClick()

        // Then
        assert(capturedTag == "Mood") { "Expected 'Mood' tag, got '$capturedTag'" }
    }

    // endregion

    // region Libido callback

    @Test
    fun libidoSelector_WHEN_starTapped_THEN_invokesCallback() {
        // Given
        var captured: Int? = null
        setContent(onLibidoChanged = { captured = it })

        // When — content description "Libido score 1"; section may need scrolling
        composeTestRule.onAllNodesWithContentDescription("Libido score 1")[0]
            .performScrollTo()
            .performClick()

        // Then
        assert(captured == 1) { "Expected libido score 1, got $captured" }
    }

    // endregion
}
