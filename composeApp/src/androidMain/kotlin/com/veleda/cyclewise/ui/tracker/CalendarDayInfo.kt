package com.veleda.cyclewise.ui.tracker

/**
 * A simple data holder representing the special status of a day on the calendar.
 */
data class CalendarDayInfo(
    val isPeriodDay: Boolean = false,
    val hasSymptoms: Boolean = false
    // TODO: Add more flags here later, like `tookMedication`
)
