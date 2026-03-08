package com.veleda.cyclewise.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import com.veleda.cyclewise.ui.theme.Dimensions
import com.veleda.cyclewise.ui.theme.LocalDimensions
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Robolectric-based Compose UI tests for [ContentContainer].
 *
 * Verifies that the wrapper renders its content and respects
 * the max-width constraint.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = com.veleda.cyclewise.RobolectricTestApp::class)
class ContentContainerTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `content WHEN placed inside ContentContainer THEN is displayed`() {
        // GIVEN — a ContentContainer wrapping a simple Text
        composeTestRule.setContent {
            CompositionLocalProvider(LocalDimensions provides Dimensions()) {
                MaterialTheme {
                    ContentContainer {
                        Text("Hello Tablet")
                    }
                }
            }
        }

        // THEN — the text is visible
        composeTestRule.onNodeWithText("Hello Tablet").assertIsDisplayed()
    }

    @Test
    fun `content WHEN custom maxWidth provided THEN width does not exceed it`() {
        // GIVEN — a ContentContainer with a narrow max width wrapping a wide Box
        val maxWidth = 200.dp
        composeTestRule.setContent {
            CompositionLocalProvider(LocalDimensions provides Dimensions()) {
                MaterialTheme {
                    ContentContainer(maxWidth = maxWidth) {
                        Box(modifier = Modifier.size(400.dp).testTag("inner")) {
                            Text("Wide Content")
                        }
                    }
                }
            }
        }

        // THEN — the inner Box width is capped at the specified max
        val bounds = composeTestRule.onNodeWithTag("inner").getUnclippedBoundsInRoot()
        val actualWidth = bounds.right - bounds.left
        assertTrue(
            "Expected width <= $maxWidth but was $actualWidth",
            actualWidth <= maxWidth
        )
    }
}
