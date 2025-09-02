package com.veleda.cyclewise.androidData.local.entities

import androidx.room.*
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

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
    @PrimaryKey val id: String, // UUID of the log
    @ColumnInfo(name = "entry_id", index = true) val entryId: String,
    @ColumnInfo(name = "symptom_id", index = true) val symptomId: String,
    @ColumnInfo(name = "severity") val severity: Int, // 1-5
    @ColumnInfo(name = "created_at") val createdAt: Instant
)
