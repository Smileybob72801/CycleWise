package com.veleda.cyclewise.domain.models

data class Symptom(
    val id: String, // UUID
    val entryId: String, // FK to DailyEntry
    val type: String, // e.g., "CRAMPS", "ACNE"
    val severity: Int, // 1-5
    val note: String? = null
)
