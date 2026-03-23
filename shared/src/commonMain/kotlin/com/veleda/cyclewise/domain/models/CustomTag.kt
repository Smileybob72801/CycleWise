package com.veleda.cyclewise.domain.models

import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * A unique tag in the user's custom tag library.
 *
 * Each tag is created once and referenced by [CustomTagLog] entries.
 * Names are unique within the library (enforced by a unique index on the entity layer).
 *
 * @property id        UUID primary key.
 * @property name      Human-readable tag name, unique across the library.
 * @property createdAt Timestamp when this tag was added to the library.
 */
data class CustomTag @OptIn(ExperimentalTime::class) constructor(
    val id: String,
    val name: String,
    val createdAt: Instant
)
