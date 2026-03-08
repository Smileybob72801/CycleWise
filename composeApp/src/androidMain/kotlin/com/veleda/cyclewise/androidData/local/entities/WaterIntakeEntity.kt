package com.veleda.cyclewise.androidData.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * Room entity for the `water_intake` table.
 *
 * Uses the ISO-8601 date string as the primary key, enforcing a **one-row-per-day**
 * constraint. Inserts use upsert semantics so re-inserting for the same date replaces
 * the existing row.
 *
 * @property date      ISO-8601 date string (e.g. "2024-01-15") serving as the natural PK.
 * @property cups      Number of water cups logged for this day (0–99).
 * @property createdAt Timestamp when this record was first persisted (epoch ms).
 * @property updatedAt Timestamp of the most recent modification (epoch ms).
 */
@OptIn(ExperimentalTime::class)
@Entity(tableName = "water_intake")
data class WaterIntakeEntity(
    @PrimaryKey val date: String,
    @ColumnInfo(name = "cups") val cups: Int,
    @ColumnInfo(name = "created_at") val createdAt: Instant,
    @ColumnInfo(name = "updated_at") val updatedAt: Instant
)
