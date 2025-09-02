package com.veleda.cyclewise.androidData.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import kotlinx.datetime.LocalDate
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
@Entity(
    tableName = "daily_entries",
    foreignKeys = [
        ForeignKey(
            entity = CycleEntity::class,
            parentColumns = ["uuid"],
            childColumns = ["cycle_id"],
            onDelete = ForeignKey.CASCADE // If a cycle is deleted, its entries are too
        )
    ]
)
data class DailyEntryEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "cycle_id", index = true) val cycleId: String,
    @ColumnInfo(name = "entry_date", index = true) val entryDate: LocalDate,
    @ColumnInfo(name = "day_in_cycle") val dayInCycle: Int,
    @ColumnInfo(name = "flow_intensity") val flowIntensity: String? = null,
    @ColumnInfo(name = "mood_score") val moodScore: Int? = null,
    @ColumnInfo(name = "energy_level") val energyLevel: Int? = null,
    @ColumnInfo(name = "libido_level") val libidoLevel: String? = null,
    val spotting: Boolean = false,
    @ColumnInfo(name = "custom_tags") val customTags: String,
    @ColumnInfo(name = "note") val note: String? = null,
    @ColumnInfo(name = "cycle_phase") val cyclePhase: String? = null,
    @ColumnInfo(name = "created_at") val createdAt: Instant,
    @ColumnInfo(name = "updated_at") val updatedAt: Instant
)
