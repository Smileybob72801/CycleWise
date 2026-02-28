package com.veleda.cyclewise.androidData.local.entities

import androidx.room.*
import androidx.room.PrimaryKey
import kotlinx.datetime.LocalDate
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * Room entity representing a single menstrual period, mapped to the `periods` table.
 *
 * Uses a dual-key strategy: an auto-generated [id] for fast Room joins and indexing,
 * and an exposed [uuid] (TEXT UNIQUE) for domain-layer identification.
 *
 * **Invariant:** [startDate] <= [endDate] when [endDate] is non-null.
 * A null [endDate] indicates an ongoing period.
 *
 * @property id        Auto-generated internal PK for Room (fast joins/indexing).
 * @property uuid      External UUID exposed to the domain layer (TEXT UNIQUE NOT NULL).
 * @property startDate The first day of this period.
 * @property endDate   The last day of this period, or null if ongoing.
 * @property createdAt Timestamp when this record was first persisted.
 * @property updatedAt Timestamp of the most recent modification.
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