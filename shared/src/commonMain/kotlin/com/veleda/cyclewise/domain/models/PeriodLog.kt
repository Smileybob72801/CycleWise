package com.veleda.cyclewise.domain.models

import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * Records the menstrual flow attributes for a single day, decoupled from [DailyEntry].
 *
 * A [PeriodLog] exists only for days where flow was actively recorded. Its presence
 * indicates the day is a period day; its absence means no flow was logged (even if the
 * date falls within a [Period]'s date range).
 *
 * **Invariant:** [flowIntensity] is always non-null — if no flow is recorded, the
 * entire [PeriodLog] is deleted rather than storing a null intensity.
 *
 * @property id            UUID primary key for this flow record.
 * @property entryId       FK to the parent [DailyEntry] for this day (CASCADE delete).
 * @property flowIntensity The subjective flow level for this day.
 * @property createdAt     Timestamp when this record was first persisted.
 * @property updatedAt     Timestamp of the most recent modification.
 */
data class PeriodLog @OptIn(ExperimentalTime::class) constructor(
    val id: String,
    val entryId: String,
    val flowIntensity: FlowIntensity,
    val createdAt: Instant,
    val updatedAt: Instant
)