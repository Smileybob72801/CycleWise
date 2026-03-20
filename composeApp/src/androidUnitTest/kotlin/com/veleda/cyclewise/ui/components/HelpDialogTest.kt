package com.veleda.cyclewise.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
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
 * Robolectric-based Compose UI tests for [HelpDialog].
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = RobolectricTestApp::class)
class HelpDialogTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val sampleTips = listOf(
        "Tap a day to view or create a daily log",
        "Swipe left or right to change months",
        "Long-press a day to mark it as a period day",
    )

    private fun setContent(
        title: String = "Test Help",
        tips: List<String> = sampleTips,
        onDismiss: () -> Unit = {},
    ) {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalDimensions provides Dimensions()) {
                MaterialTheme {
                    HelpDialog(
                        title = title,
                        tips = tips,
                        onDismiss = onDismiss,
                    )
                }
            }
        }
    }

    @Test
    fun helpDialog_WHEN_rendered_THEN_titleIsDisplayed() {
        // Given / When
        setContent()

        // Then
        composeTestRule.onNodeWithText("Test Help").assertIsDisplayed()
    }

    @Test
    fun helpDialog_WHEN_rendered_THEN_allTipsAreDisplayed() {
        // Given / When
        setContent()

        // Then
        sampleTips.forEach { tip ->
            composeTestRule.onNodeWithText(tip, substring = true).assertIsDisplayed()
        }
    }

    @Test
    fun helpDialog_WHEN_rendered_THEN_dismissButtonIsDisplayed() {
        // Given / When
        setContent()

        // Then
        composeTestRule.onNodeWithText("Got it").assertIsDisplayed()
    }

    @Test
    fun helpDialog_WHEN_dismissClicked_THEN_invokesCallback() {
        // Given
        var dismissed = false
        setContent(onDismiss = { dismissed = true })

        // When
        composeTestRule.onNodeWithText("Got it").performClick()

        // Then
        assert(dismissed) { "Expected onDismiss to be invoked" }
    }
}
