package com.veleda.cyclewise.domain.models

data class Medication(
    val id: String, // UUID
    val entryId: String, // FK to DailyEntry
    val name: String,
    val note: String? = null
)
