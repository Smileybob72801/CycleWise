package com.veleda.cyclewise.domain.models

import kotlinx.datetime.LocalDate

/**
 * Represents a single menstrual cycle.
 *
 * @property id Unique identifier fot the cycle (UUID String)
 * @property startDate The date the cycle began
 * @property endDate The date the cycle ended (nullable for ongoing cycle)
 */
data class Cycle(
    val id: String,
    val startDate: LocalDate,
    val endDate: LocalDate?
)