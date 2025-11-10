package com.veleda.cyclewise.domain.models

import kotlinx.datetime.LocalDate
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

// TODO: Standardize mood, energy, and libido. Choose 1-5 or enums for all fields.
// TODO: Change phase to an enum as well.

// TODO: Add more info fields such as mucous color and viscosity
// see https://femia.health/health-library/your-cycle/health/cervical-mucus-chart/

@OptIn(ExperimentalTime::class)
data class DailyEntry(
    val id: String, // UUID
    val entryDate: LocalDate,
    val dayInCycle: Int,
    val moodScore: Int? = null, // 1-5
    val energyLevel: Int? = null, // 1-5
    val libidoLevel: LibidoLevel? = null, // e.g., LibidoLevel.MEDIUM
    val customTags: List<String> = emptyList(), // For flexible user tags
    val note: String? = null,
    val cyclePhase: String? = null, // e.g., "FOLLICULAR"
    val createdAt: Instant,
    val updatedAt: Instant
)
