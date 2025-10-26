package com.veleda.cyclewise.domain.models

/**
 * A composite data class that holds a DailyEntry and its related
 * symptoms and medicationLogs. This is useful for the UI layer and for
 * ensuring data is saved transactionally.
 */
data class FullDailyLog(
    val entry: DailyEntry,
    val periodLog: PeriodLog? = null,
    val symptomLogs: List<SymptomLog> = emptyList(),
    val medicationLogs: List<MedicationLog> = emptyList()
)
