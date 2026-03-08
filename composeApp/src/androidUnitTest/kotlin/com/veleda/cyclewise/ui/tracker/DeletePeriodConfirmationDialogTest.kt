package com.veleda.cyclewise.ui.tracker

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
 * Robolectric-based Compose UI tests for [DeletePeriodConfirmationDialog].
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = RobolectricTestApp::class)
class DeletePeriodConfirmationDialogTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun setContent(
        showDeleteConfirmation: Boolean = false,
        periodIdToDelete: String? = null,
        onEvent: (TrackerEvent) -> Unit = {},
    ) {
        val uiState = TrackerUiState(
            showDeleteConfirmation = showDeleteConfirmation,
            periodIdToDelete = periodIdToDelete,
        )
        composeTestRule.setContent {
            CompositionLocalProvider(LocalDimensions provides Dimensions()) {
                MaterialTheme {
                    DeletePeriodConfirmationDialog(
                        uiState = uiState,
                        onEvent = onEvent,
                    )
                }
            }
        }
    }

    // region Visibility

    @Test
    fun dialog_WHEN_showDeleteFalse_THEN_notDisplayed() {
        // Given / When
        setContent(showDeleteConfirmation = false, periodIdToDelete = "period-1")

        // Then
        composeTestRule.onNodeWithText("Delete", substring = true, ignoreCase = true)
            .assertDoesNotExist()
    }

    @Test
    fun dialog_WHEN_periodIdNull_THEN_notDisplayed() {
        // Given / When
        setContent(showDeleteConfirmation = true, periodIdToDelete = null)

        // Then
        composeTestRule.onNodeWithText("Delete", substring = true, ignoreCase = true)
            .assertDoesNotExist()
    }

    @Test
    fun dialog_WHEN_showDeleteTrueAndPeriodIdNonNull_THEN_displayed() {
        // Given / When
        setContent(showDeleteConfirmation = true, periodIdToDelete = "period-1")

        // Then — the dialog title "Confirm Deletion" should be visible
        composeTestRule.onNodeWithText("Confirm Deletion").assertIsDisplayed()
    }

    // endregion

    // region Confirm button

    @Test
    fun confirmButton_WHEN_tapped_THEN_dispatchesDeleteConfirmed() {
        // Given
        val events = mutableListOf<TrackerEvent>()
        setContent(
            showDeleteConfirmation = true,
            periodIdToDelete = "period-1",
            onEvent = { events.add(it) },
        )

        // When — tap the "Delete Period" confirm button
        composeTestRule.onNodeWithText("Delete Period").performClick()

        // Then
        val confirmed = events.filterIsInstance<TrackerEvent.DeletePeriodConfirmed>()
        assert(confirmed.isNotEmpty()) { "DeletePeriodConfirmed event not dispatched" }
        assert(confirmed.first().periodId == "period-1") {
            "Expected periodId 'period-1', got '${confirmed.first().periodId}'"
        }
    }

    // endregion

    // region Dismiss button

    @Test
    fun dismissButton_WHEN_tapped_THEN_dispatchesDeleteDismissed() {
        // Given
        val events = mutableListOf<TrackerEvent>()
        setContent(
            showDeleteConfirmation = true,
            periodIdToDelete = "period-1",
            onEvent = { events.add(it) },
        )

        // When — tap the cancel button
        composeTestRule.onNodeWithText("Cancel", substring = true, ignoreCase = true)
            .performClick()

        // Then
        val dismissed = events.filterIsInstance<TrackerEvent.DeletePeriodDismissed>()
        assert(dismissed.isNotEmpty()) { "DeletePeriodDismissed event not dispatched" }
    }

    // endregion
}
