package com.veleda.cyclewise.androidData.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.datetime.LocalDate

/**
 * Room entity representing a menstrual cycle.
 *
 * @param id      Internal PK for Room (fast indexing, auto-generated).
 * @param uuid    External ID for exports/sync (TEXT UNIQUE NOT NULL).
 */
@Entity(
    tableName = "cycles",
    indices = [Index(value = ["uuid"], unique = true)]
)
data class CycleEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val uuid: String,
    val startDate: LocalDate,
    val endDate: LocalDate? = null
)