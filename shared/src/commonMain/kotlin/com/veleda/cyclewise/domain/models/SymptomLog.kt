package com.veleda.cyclewise.domain.models

import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * Represents an instance of experiencing a symptom, linked to a DailyEntry and a Symptom.
 */
data class SymptomLog @OptIn(ExperimentalTime::class) constructor(
    val id: String, // UUID of the log entry itself
    val entryId: String, // FK to DailyEntry
    val symptomId: String, // FK to the Symptom library entry
    val severity: Int, // 1-5
    val createdAt: Instant
)
