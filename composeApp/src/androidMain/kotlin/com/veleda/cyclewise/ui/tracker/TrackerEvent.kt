package com.veleda.cyclewise.ui.tracker

import com.veleda.cyclewise.domain.models.Period
import kotlinx.datetime.LocalDate

/**
 * Defines all user interactions that can occur on the Tracker screen.
 */
sealed interface TrackerEvent {
    /** The user has tapped on a specific date in the calendar. */
    data class DateClicked(val date: LocalDate, val periodForDate: Period?) : TrackerEvent

    /** The user has confirmed their date selection to create a new period. */
    object SaveSelectionClicked : TrackerEvent

    /** The user has tapped the button to end the currently ongoing period. */
    object EndPeriodClicked : TrackerEvent

    /** The user has canceled their date selection. */
    object ClearSelectionClicked : TrackerEvent

    /** The user has dismissed the bottom sheet showing the log summary. */
    object DismissLogSheet : TrackerEvent

    /** The user has confirmed the deletion of an existing period. */
    data class DeletePeriodClicked(val periodId: String) : TrackerEvent
}