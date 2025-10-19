package com.veleda.cyclewise.androidData.local.entities

import androidx.room.*
import androidx.room.PrimaryKey
import kotlinx.datetime.LocalDate
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * Room entity representing a period.
 *
 * @param id      Internal PK for Room (fast indexing, auto-generated).
 * @param uuid    External ID for exports/sync (TEXT UNIQUE NOT NULL).
 * @param startDate Start date for period (DATE NOT NULL)
 * @param endDate End date for period (DATE NULLABLE)
 * @param createdAt Record creation timestamp
 * @param updatedAt Record update timestamp
 */
@Entity(
    tableName = "periods",
    indices = [Index(value = ["uuid"], unique = true)]
)
@TypeConverters(Converters::class)
data class PeriodEntity @OptIn(ExperimentalTime::class) constructor(

    // internal only, auto-incremented for fast joins/indexing
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    // exposed to the app/sync layers
    @ColumnInfo(name = "uuid")
    val uuid: String,

    @ColumnInfo(name = "start_date")
    val startDate: LocalDate,

    @ColumnInfo(name = "end_date")
    val endDate: LocalDate? = null,

    @ColumnInfo(name = "created_at")
    val createdAt: Instant = Instant.fromEpochMilliseconds(System.currentTimeMillis()),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Instant = Instant.fromEpochMilliseconds(System.currentTimeMillis())
)