package com.veleda.cyclewise.domain.models

import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * A unique medication in the user's medication library.
 *
 * Each medication is created once and referenced by [MedicationLog] entries.
 * Names are unique within the library (enforced by a unique index on the entity layer).
 * No dosage or frequency information is tracked; logs record only that the medication was taken.
 *
 * @property id        UUID primary key.
 * @property name      Human-readable medication name, unique across the library.
 * @property createdAt Timestamp when this medication was added to the library.
 */
data class Medication @OptIn(ExperimentalTime::class) constructor(
    val id: String,
    val name: String,
    val createdAt: Instant
)
