package com.veleda.cyclewise.androidData.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.veleda.cyclewise.domain.models.FlowIntensity
import com.veleda.cyclewise.domain.models.PeriodColor
import com.veleda.cyclewise.domain.models.PeriodConsistency
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * Room entity for the `period_logs` table.
 *
 * Records menstrual flow attributes for a single day. Linked to [DailyEntryEntity]
 * via `entry_id` FK with **CASCADE delete** (deleting the parent entry removes this log).
 *
 * @property id                UUID primary key.
 * @property entryId           FK to [DailyEntryEntity.id] (indexed, CASCADE on delete).
 * @property flowIntensity     Menstrual flow level, stored as the enum name string via [Converters].
 * @property periodColor       Optional period color, stored as the enum name string via [Converters].
 * @property periodConsistency Optional period consistency, stored as the enum name string via [Converters].
 * @property createdAt         Timestamp when this record was first persisted (epoch ms).
 * @property updatedAt         Timestamp of the most recent modification (epoch ms).
 */
@OptIn(ExperimentalTime::class)
@Entity(
    tableName = "period_logs",
    foreignKeys = [
        ForeignKey(
            entity = DailyEntryEntity::class,
            parentColumns = ["id"],
            childColumns = ["entry_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class PeriodLogEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "entry_id", index = true) val entryId: String,
    @ColumnInfo(name = "flow_intensity") val flowIntensity: FlowIntensity,
    @ColumnInfo(name = "period_color") val periodColor: PeriodColor? = null,
    @ColumnInfo(name = "period_consistency") val periodConsistency: PeriodConsistency? = null,
    @ColumnInfo(name = "created_at") val createdAt: Instant,
    @ColumnInfo(name = "updated_at") val updatedAt: Instant
)