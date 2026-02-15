package com.veleda.cyclewise.ui.tracker

import com.veleda.cyclewise.domain.models.CyclePhase

/**
 * A simple data holder representing the special status of a day on the calendar.
 *
 * @property isPeriodDay   True if this date falls within a saved period's date range.
 * @property hasSymptoms   True if at least one symptom log exists for this date.
 * @property hasMedications True if at least one medication log exists for this date.
 * @property cyclePhase    Computed cycle phase for this date, or null if not determinable.
 */
data class CalendarDayInfo(
    val isPeriodDay: Boolean = false,
    val hasSymptoms: Boolean = false,
    val hasMedications: Boolean = false,
    val cyclePhase: CyclePhase? = null
)
