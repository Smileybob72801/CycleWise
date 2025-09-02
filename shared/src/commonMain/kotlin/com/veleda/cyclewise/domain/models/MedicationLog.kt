package com.veleda.cyclewise.domain.models

import kotlin.time.ExperimentalTime
import kotlin.time.Instant

data class MedicationLog @OptIn(ExperimentalTime::class) constructor(
    val id: String, // UUID
    val entryId: String, // FK to DailyEntry
    val medicationId: String,
    val createdAt: Instant
)
