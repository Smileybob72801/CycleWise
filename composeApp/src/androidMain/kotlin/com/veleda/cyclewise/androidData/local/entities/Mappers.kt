package com.veleda.cyclewise.androidData.local.entities

import com.veleda.cyclewise.domain.models.Cycle
import kotlin.time.ExperimentalTime

/** Convert Room entity → shared domain model */
@OptIn(ExperimentalTime::class)
fun CycleEntity.toDomain(): Cycle =
    Cycle(
        id        = uuid,
        startDate = startDate,
        endDate   = endDate,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

/** Convert shared domain model → Room entity (internal id left 0 for autogen) */
@OptIn(ExperimentalTime::class)
fun Cycle.toEntity(): CycleEntity =
    CycleEntity(
        id        = 0,           //  Room will auto-generate
        uuid      = id,
        startDate = startDate,
        endDate   = endDate,
        createdAt = createdAt,
        updatedAt = updatedAt
    )