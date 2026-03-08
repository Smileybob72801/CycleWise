package com.veleda.cyclewise.ui.auth

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.veleda.cyclewise.RobolectricTestApp
import com.veleda.cyclewise.ui.theme.Dimensions
import com.veleda.cyclewise.ui.theme.LocalDimensions
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Robolectric-based Compose UI tests for [WaterTrackerCounter].
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = RobolectricTestApp::class)
class WaterTrackerCounterTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun setContent(
        cups: Int = 0,
        onIncrement: () -> Unit = {},
        onDecrement: () -> Unit = {},
        yesterdayCupsForPrompt: Int? = null,
    ) {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalDimensions provides Dimensions()) {
                MaterialTheme {
                    WaterTrackerCounter(
                        cups = cups,
                        onIncrement = onIncrement,
                        onDecrement = onDecrement,
                        yesterdayCupsForPrompt = yesterdayCupsForPrompt,
                    )
                }
            }
        }
    }

    // region Count display

    @Test
    fun waterCount_WHEN_cupsIsZero_THEN_displaysZero() {
        // Given / When
        setContent(cups = 0)

        // Then
        composeTestRule.onNodeWithTag("water-count").assertIsDisplayed()
    }

    @Test
    fun waterCount_WHEN_cupsIsPositive_THEN_displaysCount() {
        // Given / When
        setContent(cups = 5)

        // Then
        composeTestRule.onNodeWithTag("water-count").assertIsDisplayed()
    }

    // endregion

    // region Decrement button

    @Test
    fun decrementButton_WHEN_cupsIsZero_THEN_isDisabled() {
        // Given / When
        setContent(cups = 0)

        // Then
        composeTestRule.onNodeWithTag("water-decrement").assertIsNotEnabled()
    }

    @Test
    fun decrementButton_WHEN_cupsIsPositive_THEN_isEnabled() {
        // Given / When
        setContent(cups = 1)

        // Then
        composeTestRule.onNodeWithTag("water-decrement").assertIsEnabled()
    }

    @Test
    fun decrementButton_WHEN_tapped_THEN_invokesCallback() {
        // Given
        var decremented = false
        setContent(cups = 3, onDecrement = { decremented = true })

        // When
        composeTestRule.onNodeWithTag("water-decrement").performClick()

        // Then
        assert(decremented) { "onDecrement callback was not invoked" }
    }

    // endregion

    // region Increment button

    @Test
    fun incrementButton_WHEN_cupsIsZero_THEN_isEnabled() {
        // Given / When
        setContent(cups = 0)

        // Then
        composeTestRule.onNodeWithTag("water-increment").assertIsEnabled()
    }

    @Test
    fun incrementButton_WHEN_tapped_THEN_invokesCallback() {
        // Given
        var incremented = false
        setContent(cups = 0, onIncrement = { incremented = true })

        // When
        composeTestRule.onNodeWithTag("water-increment").performClick()

        // Then
        assert(incremented) { "onIncrement callback was not invoked" }
    }

    // endregion

    // region Yesterday prompt

    @Test
    fun yesterdayPrompt_WHEN_null_THEN_notDisplayed() {
        // Given / When
        setContent(cups = 0, yesterdayCupsForPrompt = null)

        // Then — the yesterday message should not be in the tree
        composeTestRule.onNodeWithText("yesterday", substring = true).assertDoesNotExist()
    }

    @Test
    fun yesterdayPrompt_WHEN_nonNull_THEN_displayed() {
        // Given / When
        setContent(cups = 0, yesterdayCupsForPrompt = 3)

        // Then
        composeTestRule.onNodeWithText("yesterday", substring = true, ignoreCase = true)
            .assertIsDisplayed()
    }

    // endregion
}
