package com.veleda.cyclewise.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import com.veleda.cyclewise.ui.theme.Dimensions
import com.veleda.cyclewise.ui.theme.LocalDimensions
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Robolectric-based Compose UI tests for [SuccessAnimation].
 */
@RunWith(RobolectricTestRunner::class)
class SuccessAnimationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `SuccessAnimation WHEN visible true THEN shows content`() {
        // GIVEN
        composeTestRule.setContent {
            CompositionLocalProvider(LocalDimensions provides Dimensions()) {
                MaterialTheme {
                    SuccessAnimation(
                        visible = true,
                        onAnimationComplete = {},
                    )
                }
            }
        }

        // THEN — success content description is present
        composeTestRule.onNodeWithContentDescription("Success").assertIsDisplayed()
    }

    @Test
    fun `SuccessAnimation WHEN visible false THEN shows nothing`() {
        // GIVEN
        composeTestRule.setContent {
            CompositionLocalProvider(LocalDimensions provides Dimensions()) {
                MaterialTheme {
                    SuccessAnimation(
                        visible = false,
                        onAnimationComplete = {},
                    )
                }
            }
        }

        // THEN — no success content visible
        composeTestRule.onNodeWithContentDescription("Success").assertDoesNotExist()
    }
}
