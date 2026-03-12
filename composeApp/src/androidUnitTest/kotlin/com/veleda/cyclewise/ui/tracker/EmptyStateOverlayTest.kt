package com.veleda.cyclewise.ui.tracker

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
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
 * Robolectric-based Compose UI tests for [EmptyStateOverlay].
 *
 * Verifies that the overlay renders its scrim, icon, title, and body text,
 * and that tap dismissal triggers the [onDismissed] callback.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = RobolectricTestApp::class)
class EmptyStateOverlayTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun setContent(onDismissed: () -> Unit = {}) {
        composeTestRule.setContent {
            CompositionLocalProvider(
                LocalDimensions provides Dimensions(),
            ) {
                MaterialTheme {
                    EmptyStateOverlay(onDismissed = onDismissed)
                }
            }
        }
    }

    @Test
    fun `GIVEN overlay composed THEN scrim is displayed`() {
        // GIVEN the overlay is composed
        setContent()

        // THEN the scrim is visible
        composeTestRule.onNodeWithTag("emptyStateScrim").assertIsDisplayed()
    }

    @Test
    fun `GIVEN overlay composed THEN content is displayed`() {
        // GIVEN the overlay is composed
        setContent()

        // THEN the content container is visible
        composeTestRule.onNodeWithTag("emptyStateContent").assertIsDisplayed()
    }

    @Test
    fun `GIVEN overlay composed THEN title text is rendered`() {
        // GIVEN the overlay is composed
        setContent()

        // THEN the title text is displayed
        composeTestRule.onNodeWithText("Your Calendar Is Empty").assertIsDisplayed()
    }

    @Test
    fun `GIVEN overlay composed THEN body text is rendered`() {
        // GIVEN the overlay is composed
        setContent()

        // THEN the body text is displayed
        composeTestRule.onNodeWithText("Swipe or tap to dismiss and start logging.").assertIsDisplayed()
    }

    @Test
    fun `GIVEN overlay composed WHEN tapped THEN onDismissed is called`() {
        // GIVEN the overlay is composed with a dismiss tracker
        var dismissed = false
        setContent(onDismissed = { dismissed = true })

        // WHEN the content area is tapped
        composeTestRule.onNodeWithTag("emptyStateContent").performClick()

        // Wait for the dismiss animation to complete
        composeTestRule.waitForIdle()

        // THEN the dismiss callback fires
        assert(dismissed) { "Expected onDismissed to be called after tap" }
    }

    @Test
    fun `GIVEN overlay dismissed WHEN recomposed with state THEN overlay is gone`() {
        // GIVEN state tracking overlay visibility
        var showOverlay by mutableStateOf(true)
        composeTestRule.setContent {
            CompositionLocalProvider(
                LocalDimensions provides Dimensions(),
            ) {
                MaterialTheme {
                    if (showOverlay) {
                        EmptyStateOverlay(onDismissed = { showOverlay = false })
                    }
                }
            }
        }

        // WHEN the overlay is tapped to dismiss
        composeTestRule.onNodeWithTag("emptyStateContent").performClick()
        composeTestRule.waitForIdle()

        // THEN the overlay is no longer composed
        composeTestRule.onNodeWithTag("emptyStateScrim").assertDoesNotExist()
        composeTestRule.onNodeWithTag("emptyStateContent").assertDoesNotExist()
    }
}
