package com.veleda.cyclewise.domain.models

import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * A unique symptom type in the user's symptom library.
 *
 * Each symptom is created once and referenced by [SymptomLog] entries.
 * Names are unique within the library (enforced by a unique index on the entity layer).
 *
 * @property id        UUID primary key.
 * @property name      Human-readable symptom name, unique across the library.
 * @property category  Broad classification for UI grouping and analysis.
 * @property createdAt Timestamp when this symptom was added to the library.
 */
data class Symptom @OptIn(ExperimentalTime::class) constructor(
    val id: String,
    val name: String,
    val category: SymptomCategory,
    val createdAt: Instant
)
