package com.veleda.cyclewise.androidData.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import kotlin.time.Instant

@Entity(
    tableName = "medication_logs",
    foreignKeys = [
        ForeignKey(
            entity = DailyEntryEntity::class,
            parentColumns = ["id"],
            childColumns = ["entry_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = MedicationEntity::class,
            parentColumns = ["id"],
            childColumns = ["medication_id"],
            onDelete = ForeignKey.RESTRICT // Prevent deleting a medication if it's in use
        )
    ]
)
data class MedicationLogEntity(
    @PrimaryKey val id: String, // UUID
    @ColumnInfo(name = "entry_id", index = true) val entryId: String,
    @ColumnInfo(name = "medication_id", index = true) val medicationId: String,
    @ColumnInfo(name = "created_at") val createdAt: Instant
)
