package com.veleda.cyclewise.ui.tracker

import com.veleda.cyclewise.domain.models.Cycle
import kotlinx.datetime.LocalDate

/**
 * Defines all user interactions that can occur on the Tracker screen.
 */
sealed interface TrackerEvent {
    /** The user has tapped on a specific date in the calendar. */
    data class DateClicked(val date: LocalDate, val cycleForDate: Cycle?) : TrackerEvent

    /** The user has confirmed their date selection to create a new cycle. */
    object SaveSelectionClicked : TrackerEvent

    /** The user has tapped the button to end the currently ongoing cycle. */
    object EndCycleClicked : TrackerEvent

    /** The user has canceled their date selection. */
    object ClearSelectionClicked : TrackerEvent

    /** The user has dismissed the bottom sheet showing the log summary. */
    object DismissLogSheet : TrackerEvent
}