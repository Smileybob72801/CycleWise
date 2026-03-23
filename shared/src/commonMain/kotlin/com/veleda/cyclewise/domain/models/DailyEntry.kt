package com.veleda.cyclewise.domain.models

import kotlinx.datetime.LocalDate
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * A single day's health and wellness entry, linked to a parent period by date proximity.
 *
 * Each entry captures subjective ratings, freeform notes, and tags for one calendar day.
 * The parent period is not stored as a direct FK; instead, [dayInCycle] is calculated
 * from the nearest preceding period's start date at creation time.
 *
 * @property id         UUID primary key.
 * @property entryDate  Calendar date this entry represents (one entry per date).
 * @property dayInCycle 1-based offset from the parent period's start date, or `0` when no
 *                      parent period was found at creation time (sentinel value).
 * @property moodScore  User-rated mood on a 1 (lowest) to 5 (highest) scale, or null if unrecorded.
 * @property energyLevel User-rated energy on a 1 (lowest) to 5 (highest) scale, or null if unrecorded.
 * @property libidoScore User-rated libido on a 1 (lowest) to 5 (highest) scale, or null if unrecorded.
 * @property note       Optional free-text note for the day.
 * @property cyclePhase Computed cycle phase label (e.g., "FOLLICULAR"), or null if unset.
 * @property createdAt  Timestamp when this entry was first persisted.
 * @property updatedAt  Timestamp of the most recent modification.
 */
@OptIn(ExperimentalTime::class)
data class DailyEntry(
    val id: String,
    val entryDate: LocalDate,
    val dayInCycle: Int,
    val moodScore: Int? = null,
    val energyLevel: Int? = null,
    val libidoScore: Int? = null,
    val note: String? = null,
    val cyclePhase: String? = null,
    val createdAt: Instant,
    val updatedAt: Instant
)
