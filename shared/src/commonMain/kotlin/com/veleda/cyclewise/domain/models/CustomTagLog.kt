package com.veleda.cyclewise.domain.models

import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * Records that a custom tag was applied to a specific day.
 *
 * Links a [DailyEntry] to a [CustomTag] from the user's library.
 * Multiple [CustomTagLog] entries can exist for the same day (one per distinct tag).
 *
 * @property id        UUID primary key for this log entry.
 * @property entryId   FK to the parent [DailyEntry] (CASCADE delete).
 * @property tagId     FK to the [CustomTag] library entry (CASCADE delete).
 * @property createdAt Timestamp when this log was recorded.
 */
data class CustomTagLog @OptIn(ExperimentalTime::class) constructor(
    val id: String,
    val entryId: String,
    val tagId: String,
    val createdAt: Instant
)
