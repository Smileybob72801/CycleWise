package com.veleda.cyclewise.androidData.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import kotlinx.datetime.LocalDate
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * Room entity for the `daily_entries` table.
 *
 * Maps to the domain [DailyEntry] model. The [libidoScore] is a nullable 1-5 integer.
 * The [customTags] field is a JSON-serialized string array.
 * Timestamps ([createdAt], [updatedAt]) are stored as epoch milliseconds.
 */
@OptIn(ExperimentalTime::class)
@Entity(
    tableName = "daily_entries"
)
data class DailyEntryEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "entry_date", index = true) val entryDate: LocalDate,
    @ColumnInfo(name = "day_in_cycle") val dayInCycle: Int,
    @ColumnInfo(name = "mood_score") val moodScore: Int? = null,
    @ColumnInfo(name = "energy_level") val energyLevel: Int? = null,
    @ColumnInfo(name = "libido_score") val libidoScore: Int? = null,
    @ColumnInfo(name = "custom_tags") val customTags: String,
    @ColumnInfo(name = "note") val note: String? = null,
    @ColumnInfo(name = "cycle_phase") val cyclePhase: String? = null,
    @ColumnInfo(name = "created_at") val createdAt: Instant,
    @ColumnInfo(name = "updated_at") val updatedAt: Instant
)
