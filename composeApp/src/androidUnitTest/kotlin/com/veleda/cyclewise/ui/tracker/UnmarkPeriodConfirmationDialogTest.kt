package com.veleda.cyclewise.ui.tracker

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.veleda.cyclewise.RobolectricTestApp
import com.veleda.cyclewise.testutil.TestData
import com.veleda.cyclewise.ui.theme.Dimensions
import com.veleda.cyclewise.ui.theme.LocalDimensions
import kotlinx.datetime.LocalDate
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Robolectric-based Compose UI tests for [UnmarkPeriodDayConfirmationDialog]
 * and [UnmarkPeriodRangeConfirmationDialog].
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = RobolectricTestApp::class)
class UnmarkPeriodConfirmationDialogTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // ── Single-day dialog tests ──────────────────────────────────────

    private fun setSingleDayContent(
        showUnmarkConfirmation: Boolean = false,
        unmarkDate: LocalDate? = null,
        onEvent: (TrackerEvent) -> Unit = {},
    ) {
        val uiState = TrackerUiState(
            showUnmarkConfirmation = showUnmarkConfirmation,
            unmarkDate = unmarkDate,
            unmarkDates = emptyList(),
            unmarkDaysWithDataCount = 1,
        )
        composeTestRule.setContent {
            CompositionLocalProvider(LocalDimensions provides Dimensions()) {
                MaterialTheme {
                    UnmarkPeriodDayConfirmationDialog(
                        uiState = uiState,
                        onEvent = onEvent,
                    )
                }
            }
        }
    }

    @Test
    fun singleDayDialog_WHEN_showFalse_THEN_notDisplayed() {
        // Given / When
        setSingleDayContent(showUnmarkConfirmation = false, unmarkDate = TestData.DATE)

        // Then
        composeTestRule.onNodeWithText("Remove Period Day?").assertDoesNotExist()
    }

    @Test
    fun singleDayDialog_WHEN_unmarkDateNull_THEN_notDisplayed() {
        // Given / When
        setSingleDayContent(showUnmarkConfirmation = true, unmarkDate = null)

        // Then
        composeTestRule.onNodeWithText("Remove Period Day?").assertDoesNotExist()
    }

    @Test
    fun singleDayDialog_WHEN_showTrueAndDateNonNull_THEN_displayed() {
        // Given / When
        setSingleDayContent(showUnmarkConfirmation = true, unmarkDate = TestData.DATE)

        // Then
        composeTestRule.onNodeWithText("Remove Period Day?").assertIsDisplayed()
    }

    @Test
    fun singleDayDialog_WHEN_confirmTapped_THEN_dispatchesUnmarkDayConfirmed() {
        // Given
        val events = mutableListOf<TrackerEvent>()
        setSingleDayContent(
            showUnmarkConfirmation = true,
            unmarkDate = TestData.DATE,
            onEvent = { events.add(it) },
        )

        // When
        composeTestRule.onNodeWithText("Remove").performClick()

        // Then
        val confirmed = events.filterIsInstance<TrackerEvent.UnmarkPeriodDayConfirmed>()
        assert(confirmed.isNotEmpty()) { "UnmarkPeriodDayConfirmed event not dispatched" }
        assert(confirmed.first().date == TestData.DATE) {
            "Expected date ${TestData.DATE}, got ${confirmed.first().date}"
        }
    }

    @Test
    fun singleDayDialog_WHEN_cancelTapped_THEN_dispatchesUnmarkDismissed() {
        // Given
        val events = mutableListOf<TrackerEvent>()
        setSingleDayContent(
            showUnmarkConfirmation = true,
            unmarkDate = TestData.DATE,
            onEvent = { events.add(it) },
        )

        // When
        composeTestRule.onNodeWithText("Cancel").performClick()

        // Then
        val dismissed = events.filterIsInstance<TrackerEvent.UnmarkPeriodDismissed>()
        assert(dismissed.isNotEmpty()) { "UnmarkPeriodDismissed event not dispatched" }
    }

    // ── Multi-day dialog tests ───────────────────────────────────────

    private fun setMultiDayContent(
        showUnmarkConfirmation: Boolean = false,
        unmarkDates: List<LocalDate> = emptyList(),
        unmarkDaysWithDataCount: Int = 0,
        onEvent: (TrackerEvent) -> Unit = {},
    ) {
        val uiState = TrackerUiState(
            showUnmarkConfirmation = showUnmarkConfirmation,
            unmarkDate = null,
            unmarkDates = unmarkDates,
            unmarkDaysWithDataCount = unmarkDaysWithDataCount,
        )
        composeTestRule.setContent {
            CompositionLocalProvider(LocalDimensions provides Dimensions()) {
                MaterialTheme {
                    UnmarkPeriodRangeConfirmationDialog(
                        uiState = uiState,
                        onEvent = onEvent,
                    )
                }
            }
        }
    }

    @Test
    fun multiDayDialog_WHEN_showFalse_THEN_notDisplayed() {
        // Given / When
        setMultiDayContent(
            showUnmarkConfirmation = false,
            unmarkDates = listOf(TestData.DATE, TestData.DATE_YESTERDAY),
        )

        // Then
        composeTestRule.onNodeWithText("Remove Period Days?").assertDoesNotExist()
    }

    @Test
    fun multiDayDialog_WHEN_unmarkDatesEmpty_THEN_notDisplayed() {
        // Given / When
        setMultiDayContent(showUnmarkConfirmation = true, unmarkDates = emptyList())

        // Then
        composeTestRule.onNodeWithText("Remove Period Days?").assertDoesNotExist()
    }

    @Test
    fun multiDayDialog_WHEN_showTrueAndDatesNonEmpty_THEN_displayed() {
        // Given / When
        setMultiDayContent(
            showUnmarkConfirmation = true,
            unmarkDates = listOf(TestData.DATE, TestData.DATE_YESTERDAY),
            unmarkDaysWithDataCount = 1,
        )

        // Then
        composeTestRule.onNodeWithText("Remove Period Days?").assertIsDisplayed()
    }

    @Test
    fun multiDayDialog_WHEN_confirmTapped_THEN_dispatchesUnmarkRangeConfirmed() {
        // Given
        val events = mutableListOf<TrackerEvent>()
        val dates = listOf(TestData.DATE, TestData.DATE_YESTERDAY)
        setMultiDayContent(
            showUnmarkConfirmation = true,
            unmarkDates = dates,
            unmarkDaysWithDataCount = 1,
            onEvent = { events.add(it) },
        )

        // When — tap the "Remove 2 Days" button
        composeTestRule.onNodeWithText("Remove 2 Days").performClick()

        // Then
        val confirmed = events.filterIsInstance<TrackerEvent.UnmarkPeriodRangeConfirmed>()
        assert(confirmed.isNotEmpty()) { "UnmarkPeriodRangeConfirmed event not dispatched" }
        assert(confirmed.first().dates == dates) {
            "Expected dates $dates, got ${confirmed.first().dates}"
        }
    }

    @Test
    fun multiDayDialog_WHEN_cancelTapped_THEN_dispatchesUnmarkDismissed() {
        // Given
        val events = mutableListOf<TrackerEvent>()
        setMultiDayContent(
            showUnmarkConfirmation = true,
            unmarkDates = listOf(TestData.DATE, TestData.DATE_YESTERDAY),
            unmarkDaysWithDataCount = 1,
            onEvent = { events.add(it) },
        )

        // When
        composeTestRule.onNodeWithText("Cancel").performClick()

        // Then
        val dismissed = events.filterIsInstance<TrackerEvent.UnmarkPeriodDismissed>()
        assert(dismissed.isNotEmpty()) { "UnmarkPeriodDismissed event not dispatched" }
    }
}
