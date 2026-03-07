package com.veleda.cyclewise.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.veleda.cyclewise.ui.theme.Dimensions
import com.veleda.cyclewise.ui.theme.LocalDimensions
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Robolectric-based test for the [shimmer] modifier.
 */
@RunWith(RobolectricTestRunner::class)
class ShimmerModifierTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `shimmer modifier WHEN applied to Box THEN renders without crash`() {
        // GIVEN
        composeTestRule.setContent {
            CompositionLocalProvider(LocalDimensions provides Dimensions()) {
                MaterialTheme {
                    Box(modifier = Modifier.size(100.dp).shimmer())
                }
            }
        }

        // THEN — no crash
        composeTestRule.waitForIdle()
    }
}
