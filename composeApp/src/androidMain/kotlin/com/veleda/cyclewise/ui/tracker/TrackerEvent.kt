package com.veleda.cyclewise.ui.tracker

import com.veleda.cyclewise.domain.models.Period
import kotlinx.datetime.LocalDate

/**
 * Defines all user interactions that can occur on the Tracker screen.
 */
sealed interface TrackerEvent {
    /** The user has tapped on a specific date in the calendar. */
    data class DateTapped(val date: LocalDate, val periodForDate: Period?) : TrackerEvent

    /** The user long-pressed a day to mark or unmark it as a period day. */
    data class PeriodMarkDay(val date: LocalDate) : TrackerEvent

    /** The user has dismissed the bottom sheet showing the log summary. */
    object DismissLogSheet : TrackerEvent

    /** The user has confirmed the deletion of an existing period. */
    data class DeletePeriodClicked(val periodId: String) : TrackerEvent

    /** Needed for auto-close period logic. */
    object ScreenEntered : TrackerEvent
}