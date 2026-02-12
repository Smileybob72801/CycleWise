package com.veleda.cyclewise.domain.models

import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * Records a single occurrence of a symptom on a specific day.
 *
 * Links a [DailyEntry] to a [Symptom] from the user's library with a severity rating.
 * Multiple [SymptomLog] entries can exist for the same day (one per distinct symptom).
 *
 * @property id        UUID primary key for this log entry.
 * @property entryId   FK to the parent [DailyEntry] (CASCADE delete).
 * @property symptomId FK to the [Symptom] library entry (RESTRICT delete).
 * @property severity  User-rated severity on a 1 (mild) to 5 (severe) scale.
 * @property createdAt Timestamp when this log was recorded.
 */
data class SymptomLog @OptIn(ExperimentalTime::class) constructor(
    val id: String,
    val entryId: String,
    val symptomId: String,
    val severity: Int,
    val createdAt: Instant
)
