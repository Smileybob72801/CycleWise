package com.veleda.cyclewise.androidData.local.entities

import androidx.room.*
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * Room entity for the `symptom_logs` table.
 *
 * Records a symptom occurrence for a single day. FK to `daily_entries` (CASCADE)
 * and to `symptom_library` (RESTRICT — cannot delete a symptom type while logs reference it).
 * Severity is an integer in the range 1-5.
 */
@OptIn(ExperimentalTime::class)
@Entity(
    tableName = "symptom_logs",
    foreignKeys = [
        ForeignKey(
            entity = DailyEntryEntity::class,
            parentColumns = ["id"],
            childColumns = ["entry_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = SymptomEntity::class,
            parentColumns = ["id"],
            childColumns = ["symptom_id"],
            onDelete = ForeignKey.RESTRICT
        )
    ]
)
data class SymptomLogEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "entry_id", index = true) val entryId: String,
    @ColumnInfo(name = "symptom_id", index = true) val symptomId: String,
    @ColumnInfo(name = "severity") val severity: Int,
    @ColumnInfo(name = "created_at") val createdAt: Instant
)
