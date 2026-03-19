package com.veleda.cyclewise.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createComposeRule
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
 * Robolectric-based Compose UI tests for [HelpButton].
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = RobolectricTestApp::class)
class HelpButtonTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun setContent(
        onClick: () -> Unit = {},
        contentDescription: String = "Usage help for Tracker",
    ) {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalDimensions provides Dimensions()) {
                MaterialTheme {
                    HelpButton(
                        onClick = onClick,
                        contentDescription = contentDescription,
                    )
                }
            }
        }
    }

    @Test
    fun helpButton_WHEN_rendered_THEN_isDisplayed() {
        // Given / When
        setContent()

        // Then
        composeTestRule.onNode(
            hasContentDescription("Usage help for Tracker"),
            useUnmergedTree = true,
        ).assertIsDisplayed()
    }

    @Test
    fun helpButton_WHEN_clicked_THEN_invokesCallback() {
        // Given
        var clicked = false
        setContent(onClick = { clicked = true })

        // When
        composeTestRule.onNode(
            hasContentDescription("Usage help for Tracker"),
            useUnmergedTree = true,
        ).performClick()

        // Then
        assert(clicked) { "Expected onClick to be invoked" }
    }
}
