package com.veleda.cyclewise.domain.models

import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * Records that a medication was taken on a specific day.
 *
 * This is a boolean "taken" record — no dosage or frequency information is stored.
 * Links a [DailyEntry] to a [Medication] from the user's library.
 * Multiple [MedicationLog] entries can exist for the same day (one per distinct medication).
 *
 * @property id           UUID primary key for this log entry.
 * @property entryId      FK to the parent [DailyEntry] (CASCADE delete).
 * @property medicationId FK to the [Medication] library entry (RESTRICT delete).
 * @property createdAt    Timestamp when this log was recorded.
 */
data class MedicationLog @OptIn(ExperimentalTime::class) constructor(
    val id: String,
    val entryId: String,
    val medicationId: String,
    val createdAt: Instant
)
