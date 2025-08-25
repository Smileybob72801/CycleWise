package com.veleda.cyclewise.androidData.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import androidx.room.*
import com.veleda.cyclewise.domain.models.SymptomCategory
import kotlin.time.Instant

@Entity(
    tableName = "symptom_library",
    indices = [Index(value = ["name"], unique = true)]
)
data class SymptomEntity(
    @PrimaryKey val id: String, // UUID
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "category") val category: SymptomCategory,
    @ColumnInfo(name = "created_at") val createdAt: Instant
)
