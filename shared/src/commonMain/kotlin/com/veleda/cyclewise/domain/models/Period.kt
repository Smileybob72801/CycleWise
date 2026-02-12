package com.veleda.cyclewise.domain.models

import kotlinx.datetime.LocalDate
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * Represents a single menstrual period (a contiguous range of period days).
 *
 * **Invariant:** [startDate] <= [endDate] when [endDate] is non-null.
 * A null [endDate] indicates an ongoing (not yet ended) period.
 *
 * Periods are sorted by [startDate] descending when returned from [PeriodRepository.getAllPeriods].
 * The cycle length is measured as the number of days from this period's [startDate] to the
 * next period's start date.
 *
 * @property id        External UUID exposed to the domain and UI layers.
 * @property startDate The first day of this period.
 * @property endDate   The last day of this period, or null if the period is still ongoing.
 * @property createdAt Timestamp when this record was first persisted.
 * @property updatedAt Timestamp of the most recent modification.
 */
data class Period @OptIn(ExperimentalTime::class) constructor(
    val id: String,
    val startDate: LocalDate,
    val endDate: LocalDate?,
    val createdAt: Instant,
    val updatedAt: Instant
)
