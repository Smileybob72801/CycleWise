package com.veleda.cyclewise.domain.models

import kotlinx.datetime.LocalDate
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
data class DailyEntry(
    val id: String, // UUID
    val entryDate: LocalDate,
    val dayInCycle: Int,
    val flowIntensity: FlowIntensity? = null, // e.g., FlowIntensity.MEDIUM
    val moodScore: Int? = null, // 1-5
    val energyLevel: Int? = null, // 1-5
    val libidoLevel: LibidoLevel? = null, // e.g., LibidoLevel.MEDIUM
    val spotting: Boolean = false,
    val customTags: List<String> = emptyList(), // For flexible user tags
    val note: String? = null,
    val cyclePhase: String? = null, // e.g., "FOLLICULAR"
    val createdAt: Instant,
    val updatedAt: Instant
)
