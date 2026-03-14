package com.veleda.cyclewise.ui.log

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
 * Robolectric-based Compose UI tests for [UnmarkPeriodDayDialog].
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = RobolectricTestApp::class)
class UnmarkPeriodDayDialogTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun setContent(
        showDialog: Boolean = false,
        onConfirm: () -> Unit = {},
        onDismiss: () -> Unit = {},
    ) {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalDimensions provides Dimensions()) {
                MaterialTheme {
                    UnmarkPeriodDayDialog(
                        showDialog = showDialog,
                        onConfirm = onConfirm,
                        onDismiss = onDismiss,
                    )
                }
            }
        }
    }

    @Test
    fun dialog_WHEN_showDialogFalse_THEN_notDisplayed() {
        // Given / When
        setContent(showDialog = false)

        // Then
        composeTestRule.onNodeWithText("Remove Period Day?").assertDoesNotExist()
    }

    @Test
    fun dialog_WHEN_showDialogTrue_THEN_displayed() {
        // Given / When
        setContent(showDialog = true)

        // Then
        composeTestRule.onNodeWithText("Remove Period Day?").assertIsDisplayed()
    }

    @Test
    fun confirmButton_WHEN_tapped_THEN_callsOnConfirm() {
        // Given
        var confirmed = false
        setContent(showDialog = true, onConfirm = { confirmed = true })

        // When
        composeTestRule.onNodeWithText("Remove").performClick()

        // Then
        assert(confirmed) { "onConfirm should have been called" }
    }

    @Test
    fun cancelButton_WHEN_tapped_THEN_callsOnDismiss() {
        // Given
        var dismissed = false
        setContent(showDialog = true, onDismiss = { dismissed = true })

        // When
        composeTestRule.onNodeWithText("Cancel").performClick()

        // Then
        assert(dismissed) { "onDismiss should have been called" }
    }
}
