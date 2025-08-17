package com.veleda.cyclewise.androidData.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "symptoms",
    foreignKeys = [
        ForeignKey(
            entity = DailyEntryEntity::class,
            parentColumns = ["id"],
            childColumns = ["entry_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class SymptomEntity(
    @PrimaryKey val id: String, // UUID
    @ColumnInfo(name = "entry_id", index = true) val entryId: String,
    @ColumnInfo(name = "symptom_type") val type: String, // e.g., "CRAMPS", "ACNE"
    val severity: Int, // 1-5
    val note: String? = null
)