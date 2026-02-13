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
 * via `entry_id` FK with CASCADE delete. The [flowIntensity], [periodColor], and
 * [periodConsistency] enums are stored as their name strings via [Converters].
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