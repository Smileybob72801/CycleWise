package com.veleda.cyclewise.ui.tracker

import com.veleda.cyclewise.domain.models.HeatmapMetric
import kotlinx.datetime.LocalDate

/**
 * Defines all user interactions that can occur on the Tracker screen.
 */
sealed interface TrackerEvent {
    /** The user selected or deselected a heatmap metric overlay. */
    data class SelectHeatmapMetric(val metric: HeatmapMetric?) : TrackerEvent
    /** The user tapped a day in the calendar. */
    data class DayTapped(val date: LocalDate) : TrackerEvent

    /** The user long-pressed a day to mark or unmark it as a period day. */
    data class PeriodMarkDay(val date: LocalDate) : TrackerEvent

    /** The user long-pressed [anchorDate] and dragged to [releaseDate], requesting a period range operation. */
    data class PeriodRangeDragged(val anchorDate: LocalDate, val releaseDate: LocalDate) : TrackerEvent

    /** The user has dismissed the bottom sheet showing the log summary. */
    data object DismissLogSheet : TrackerEvent

    /** The user tapped the Edit button in the bottom sheet. */
    data class EditLogClicked(val date: LocalDate) : TrackerEvent

    /** The user tapped the Delete button — shows confirmation dialog. */
    data class DeletePeriodRequested(val periodId: String) : TrackerEvent

    /** The user confirmed deletion in the dialog. */
    data class DeletePeriodConfirmed(val periodId: String) : TrackerEvent

    /** The user dismissed the deletion confirmation dialog. */
    data object DeletePeriodDismissed : TrackerEvent

    /** Dispatched when the screen is first composed, triggering auto-close period logic. */
    data object ScreenEntered : TrackerEvent

    // ── Educational ──────────────────────────────────────────────────

    /** The user tapped an info button to view educational content for the given [contentTag]. */
    data class ShowEducationalSheet(val contentTag: String) : TrackerEvent

    /** The user dismissed the educational bottom sheet. */
    data object DismissEducationalSheet : TrackerEvent
}

/**
 * One-time side effects emitted by [TrackerViewModel].
 */
sealed interface TrackerEffect {
    /** Navigate to the daily log detail screen for the given [date]. */
    data class NavigateToDailyLog(val date: LocalDate) : TrackerEffect

    /** A period day was marked or unmarked, triggering a success animation. */
    data object PeriodMarked : TrackerEffect
}
