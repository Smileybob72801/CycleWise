package com.veleda.cyclewise.domain.models

/**
 * A composite data class that holds a DailyEntry and its related
 * symptoms and medications. This is useful for the UI layer and for
 * ensuring data is saved transactionally.
 */
data class FullDailyLog(
    val entry: DailyEntry,
    val symptoms: List<Symptom> = emptyList(),
    val medications: List<Medication> = emptyList()
)
