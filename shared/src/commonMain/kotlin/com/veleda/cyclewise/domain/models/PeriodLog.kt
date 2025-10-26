package com.veleda.cyclewise.domain.models

import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * Represents the flow attributes recorded for a single day,
 * decoupled from the core DailyEntry model.
 */
data class PeriodLog @OptIn(ExperimentalTime::class) constructor(
    val id: String, // UUID for this flow record
    val entryId: String, // FK to the DailyEntry for this day
    val flowIntensity: FlowIntensity, // Must be non-null if this log exists
    val createdAt: Instant,
    val updatedAt: Instant
)