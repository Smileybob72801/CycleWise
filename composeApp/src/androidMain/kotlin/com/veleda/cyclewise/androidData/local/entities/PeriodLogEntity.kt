package com.veleda.cyclewise.androidData.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.veleda.cyclewise.domain.models.FlowIntensity
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
@Entity(
    tableName = "period_logs",
    foreignKeys = [
        ForeignKey(
            entity = DailyEntryEntity::class,
            parentColumns = ["id"],
            childColumns = ["entry_id"],
            onDelete = ForeignKey.CASCADE // PeriodLog is deleted if DailyEntry is deleted
        )
    ]
)
data class PeriodLogEntity(
    @PrimaryKey val id: String, // UUID
    @ColumnInfo(name = "entry_id", index = true) val entryId: String,
    @ColumnInfo(name = "flow_intensity") val flowIntensity: FlowIntensity,
    @ColumnInfo(name = "created_at") val createdAt: Instant,
    @ColumnInfo(name = "updated_at") val updatedAt: Instant
)