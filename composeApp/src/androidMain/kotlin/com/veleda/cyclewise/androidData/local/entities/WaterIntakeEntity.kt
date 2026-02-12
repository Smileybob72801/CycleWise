package com.veleda.cyclewise.androidData.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * Room entity for the `water_intake` table.
 *
 * Uses the ISO-8601 date string as the primary key (one row per day, upsert semantics).
 * Timestamps are stored as epoch milliseconds.
 */
@OptIn(ExperimentalTime::class)
@Entity(tableName = "water_intake")
data class WaterIntakeEntity(
    @PrimaryKey val date: String,
    @ColumnInfo(name = "cups") val cups: Int,
    @ColumnInfo(name = "created_at") val createdAt: Instant,
    @ColumnInfo(name = "updated_at") val updatedAt: Instant
)
