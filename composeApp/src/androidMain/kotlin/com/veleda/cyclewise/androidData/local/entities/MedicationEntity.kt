package com.veleda.cyclewise.androidData.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "medications",
    foreignKeys = [
        ForeignKey(
            entity = DailyEntryEntity::class,
            parentColumns = ["id"],
            childColumns = ["entry_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class MedicationEntity(
    @PrimaryKey val id: String, // UUID
    @ColumnInfo(name = "entry_id", index = true) val entryId: String,
    @ColumnInfo(name = "medication_name") val name: String,
    val note: String? = null
)
