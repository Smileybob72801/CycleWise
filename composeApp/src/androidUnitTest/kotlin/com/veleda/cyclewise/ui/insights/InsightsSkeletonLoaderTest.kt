package com.veleda.cyclewise.ui.insights

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
 * Robolectric-based test for [InsightsSkeletonLoader].
 */
@RunWith(RobolectricTestRunner::class)
class InsightsSkeletonLoaderTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `InsightsSkeletonLoader WHEN rendered THEN shows loading content description`() {
        // GIVEN
        composeTestRule.setContent {
            CompositionLocalProvider(LocalDimensions provides Dimensions()) {
                MaterialTheme {
                    InsightsSkeletonLoader()
                }
            }
        }

        // THEN — accessible loading description is present
        composeTestRule.onNodeWithContentDescription("Loading").assertIsDisplayed()
    }
}
