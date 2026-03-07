package com.veleda.cyclewise.ui.log

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
 * Robolectric-based test for [DailyLogSkeletonLoader].
 */
@RunWith(RobolectricTestRunner::class)
class DailyLogSkeletonLoaderTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `DailyLogSkeletonLoader WHEN rendered THEN shows loading content description`() {
        // GIVEN
        composeTestRule.setContent {
            CompositionLocalProvider(LocalDimensions provides Dimensions()) {
                MaterialTheme {
                    DailyLogSkeletonLoader()
                }
            }
        }

        // THEN — accessible loading description is present
        composeTestRule.onNodeWithContentDescription("Loading").assertIsDisplayed()
    }
}
