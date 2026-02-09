package com.veleda.cyclewise.domain.models

import kotlinx.datetime.LocalDate
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * Represents a single day's water intake record.
 * Uses [date] as the natural key (one row per day).
 */
data class WaterIntake @OptIn(ExperimentalTime::class) constructor(
    val date: LocalDate,
    val cups: Int,
    val createdAt: Instant,
    val updatedAt: Instant
)
