package com.veleda.cyclewise.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import com.veleda.cyclewise.R
import com.veleda.cyclewise.ui.theme.Dimensions
import com.veleda.cyclewise.ui.theme.LocalDimensions
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Robolectric-based Compose UI tests for [LottieAnimationBox].
 */
@RunWith(RobolectricTestRunner::class)
class LottieAnimationBoxTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `LottieAnimationBox WHEN rendered THEN does not crash`() {
        // GIVEN
        composeTestRule.setContent {
            CompositionLocalProvider(LocalDimensions provides Dimensions()) {
                MaterialTheme {
                    LottieAnimationBox(
                        animationResId = R.raw.anim_loading_general,
                        contentDescription = "Test animation",
                    )
                }
            }
        }

        // THEN — composable renders without crashing
        composeTestRule.waitForIdle()
    }

    @Test
    fun `LottieAnimationBox WHEN contentDescription set THEN accessible`() {
        // GIVEN
        composeTestRule.setContent {
            CompositionLocalProvider(LocalDimensions provides Dimensions()) {
                MaterialTheme {
                    LottieAnimationBox(
                        animationResId = R.raw.anim_loading_general,
                        contentDescription = "Loading animation",
                    )
                }
            }
        }

        // THEN — content description is present
        composeTestRule.onNodeWithContentDescription("Loading animation").assertIsDisplayed()
    }
}
