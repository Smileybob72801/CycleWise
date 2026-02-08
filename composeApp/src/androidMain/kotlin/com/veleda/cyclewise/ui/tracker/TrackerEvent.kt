package com.veleda.cyclewise.ui.tracker

import kotlinx.datetime.LocalDate

/**
 * Defines all user interactions that can occur on the Tracker screen.
 */
sealed interface TrackerEvent {
    /** The user tapped a day in the calendar. */
    data class DayTapped(val date: LocalDate) : TrackerEvent

    /** The user long-pressed a day to mark or unmark it as a period day. */
    data class PeriodMarkDay(val date: LocalDate) : TrackerEvent

    /** The user has dismissed the bottom sheet showing the log summary. */
    object DismissLogSheet : TrackerEvent

    /** The user tapped the Edit button in the bottom sheet. */
    data class EditLogClicked(val date: LocalDate) : TrackerEvent

    /** The user tapped the Delete button — shows confirmation dialog. */
    data class DeletePeriodRequested(val periodId: String) : TrackerEvent

    /** The user confirmed deletion in the dialog. */
    data class DeletePeriodConfirmed(val periodId: String) : TrackerEvent

    /** The user dismissed the deletion confirmation dialog. */
    object DeletePeriodDismissed : TrackerEvent

    /** Needed for auto-close period logic. */
    object ScreenEntered : TrackerEvent
}

/**
 * One-time side effects emitted by [TrackerViewModel].
 */
sealed interface TrackerEffect {
    data class NavigateToDailyLog(val date: LocalDate, val isPeriodDay: Boolean) : TrackerEffect
}
