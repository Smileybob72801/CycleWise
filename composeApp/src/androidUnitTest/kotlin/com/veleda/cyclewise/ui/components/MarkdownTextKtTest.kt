package com.veleda.cyclewise.ui.components

import android.widget.TextView
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.Modifier
import com.veleda.cyclewise.ui.theme.Dimensions
import com.veleda.cyclewise.ui.theme.LocalDimensions
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertTrue

/**
 * Robolectric-based Compose UI tests for [MarkdownText].
 *
 * The library wraps an Android [TextView] via AndroidView, so standard Compose
 * text matchers cannot see the rendered content. These tests verify the composable
 * renders without crashing for various inputs.
 */
@RunWith(RobolectricTestRunner::class)
class MarkdownTextKtTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun setContent(text: String) {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalDimensions provides Dimensions()) {
                MaterialTheme {
                    MarkdownText(
                        text = text,
                        modifier = Modifier.testTag("markdown"),
                    )
                }
            }
        }
    }

    @Test
    fun `MarkdownText WHEN plain text THEN renders without crash`() {
        // GIVEN
        setContent("Hello World")

        // THEN — composable renders
        composeTestRule.onNodeWithTag("markdown").assertExists()
    }

    @Test
    fun `MarkdownText WHEN bold text THEN renders without crash`() {
        // GIVEN — bold markdown
        setContent("This is **bold** text")

        // THEN — composable renders
        composeTestRule.onNodeWithTag("markdown").assertExists()
    }

    @Test
    fun `MarkdownText WHEN empty string THEN does not crash`() {
        // GIVEN — empty string
        setContent("")

        // THEN — no crash, composable renders
        composeTestRule.waitForIdle()
    }

    @Test
    fun `MarkdownText WHEN bullet list THEN renders without crash`() {
        // GIVEN — markdown bullet list
        setContent("Items:\n- First\n- Second")

        // THEN — composable renders
        composeTestRule.onNodeWithTag("markdown").assertExists()
    }
}
