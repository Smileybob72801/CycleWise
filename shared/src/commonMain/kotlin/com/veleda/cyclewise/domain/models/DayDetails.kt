package com.veleda.cyclewise.domain.models

/**
 * UI-agnostic summary of a single calendar day's status, derived from periods and logs.
 *
 * Emitted by [PeriodRepository.observeDayDetails] as the single source of truth
 * for the calendar UI. Each flag indicates whether the corresponding data type
 * has been recorded for this day.
 *
 * @property isPeriodDay          True if this date falls within any saved period's date range.
 * @property hasLoggedSymptoms    True if at least one [SymptomLog] exists for this date.
 * @property hasLoggedMedications True if at least one [MedicationLog] exists for this date.
 * @property cyclePhase           Computed cycle phase for this date, or null if not determinable.
 */
data class DayDetails(
    val isPeriodDay: Boolean = false,
    val hasLoggedSymptoms: Boolean = false,
    val hasLoggedMedications: Boolean = false,
    val cyclePhase: CyclePhase? = null
)