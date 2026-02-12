package com.veleda.cyclewise.domain.models

import kotlinx.datetime.LocalDate
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * A single day's water intake record.
 *
 * Uses [date] as the natural primary key (one row per day, upsert semantics).
 * Water tracking is available both on the lock screen (via [LockedWaterDraft])
 * and the daily log screen; drafts are synced into the encrypted database on unlock.
 *
 * @property date      Calendar date this record represents (PK, ISO-8601 string in DB).
 * @property cups      Number of cups logged for this day (non-negative).
 * @property createdAt Timestamp when this record was first persisted.
 * @property updatedAt Timestamp of the most recent modification.
 */
data class WaterIntake @OptIn(ExperimentalTime::class) constructor(
    val date: LocalDate,
    val cups: Int,
    val createdAt: Instant,
    val updatedAt: Instant
)
