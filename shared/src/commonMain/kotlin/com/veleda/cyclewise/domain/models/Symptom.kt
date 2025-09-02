package com.veleda.cyclewise.domain.models

import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * Represents a unique symptom type in the user's library.
 */
data class Symptom @OptIn(ExperimentalTime::class) constructor(
    val id: String, // UUID
    val name: String,
    val category: SymptomCategory,
    val createdAt: Instant
)
