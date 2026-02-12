package com.veleda.cyclewise.androidData.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import kotlin.time.Instant

/**
 * Room entity for the `medication_logs` table.
 *
 * Records that a medication was taken on a single day. FK to `daily_entries` (CASCADE)
 * and to `medication_library` (RESTRICT — cannot delete a medication type while logs reference it).
 */
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
            onDelete = ForeignKey.RESTRICT
        )
    ]
)
data class MedicationLogEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "entry_id", index = true) val entryId: String,
    @ColumnInfo(name = "medication_id", index = true) val medicationId: String,
    @ColumnInfo(name = "created_at") val createdAt: Instant
)
