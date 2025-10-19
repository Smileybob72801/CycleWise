package com.veleda.cyclewise.domain.models

import kotlinx.datetime.LocalDate
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * Represents a single period.
 *
 * @property id        External UUID (TEXT UNIQUE) for this cycle
 * @property startDate The date the cycle began
 * @property endDate   The date the cycle ended (nullable for ongoing cycle)
 * @property createdAt Timestamp when this record was first created
 * @property updatedAt Timestamp of the last update to this record
 */
data class Period @OptIn(ExperimentalTime::class) constructor(
    val id: String,
    val startDate: LocalDate,
    val endDate: LocalDate?,
    val createdAt: Instant,
    val updatedAt: Instant
)
