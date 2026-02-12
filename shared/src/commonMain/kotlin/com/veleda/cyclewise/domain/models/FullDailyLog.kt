package com.veleda.cyclewise.domain.models

/**
 * Composite snapshot of all data recorded for a single day.
 *
 * Aggregates a [DailyEntry] with its related flow, symptom, and medication logs.
 * Used as the unit of transactional save in [PeriodRepository.saveFullLog] and as
 * the data source for the daily log editing screen.
 *
 * @property entry          The core daily entry (mood, energy, tags, notes).
 * @property periodLog      Flow intensity for this day, or null if no flow was recorded.
 * @property symptomLogs    Symptoms logged for this day; empty list means none recorded.
 * @property medicationLogs Medications taken on this day; empty list means none recorded.
 */
data class FullDailyLog(
    val entry: DailyEntry,
    val periodLog: PeriodLog? = null,
    val symptomLogs: List<SymptomLog> = emptyList(),
    val medicationLogs: List<MedicationLog> = emptyList()
)
