package com.veleda.cyclewise.androidData.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * Room entity for the `medication_library` table.
 *
 * Each row is a unique medication type. The [name] column has a UNIQUE index.
 */
@OptIn(ExperimentalTime::class)
@Entity(
    tableName = "medication_library",
    indices = [Index(value = ["name"], unique = true)]
)
data class MedicationEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "created_at") val createdAt: Instant
)
