package com.veleda.cyclewise.domain.models

import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * Records the menstrual flow attributes for a single day, decoupled from [DailyEntry].
 *
 * A [PeriodLog]'s presence alone indicates the day is a period day — it can exist with
 * a null [flowIntensity] when the user has toggled "on period" without selecting a
 * specific flow level. Its absence means no period was logged (even if the date falls
 * within a [Period]'s date range).
 *
 * @property id                UUID primary key for this flow record.
 * @property entryId           FK to the parent [DailyEntry] for this day (CASCADE delete).
 * @property flowIntensity     The subjective flow level for this day, or null if unrecorded.
 * @property periodColor       Observed color of the flow, or null if unrecorded.
 * @property periodConsistency Observed consistency/texture of the flow, or null if unrecorded.
 * @property createdAt         Timestamp when this record was first persisted.
 * @property updatedAt         Timestamp of the most recent modification.
 */
data class PeriodLog @OptIn(ExperimentalTime::class) constructor(
    val id: String,
    val entryId: String,
    val flowIntensity: FlowIntensity? = null,
    val periodColor: PeriodColor? = null,
    val periodConsistency: PeriodConsistency? = null,
    val createdAt: Instant,
    val updatedAt: Instant
) {
    /**
     * Returns `true` if this log contains any user-entered period detail data
     * (flow intensity, color, or consistency). A [PeriodLog] can exist with all
     * fields null when the user toggled "on period" without selecting any details.
     */
    fun hasData(): Boolean =
        flowIntensity != null || periodColor != null || periodConsistency != null
}