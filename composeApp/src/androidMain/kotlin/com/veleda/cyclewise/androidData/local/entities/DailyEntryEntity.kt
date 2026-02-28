package com.veleda.cyclewise.androidData.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import kotlinx.datetime.LocalDate
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * Room entity for the `daily_entries` table.
 *
 * Maps to the domain [DailyEntry] model.
 * [dayInCycle] is 1-based when a parent period exists, or `0` as a sentinel when no
 * parent period was found at creation time.
 *
 * @property id          UUID primary key.
 * @property entryDate   Calendar date this entry represents (indexed).
 * @property dayInCycle  1-based offset from the parent period's start date, or 0 if unlinked.
 * @property moodScore   User-rated mood on a 1–5 scale, or null if unrecorded.
 * @property energyLevel User-rated energy on a 1–5 scale, or null if unrecorded.
 * @property libidoScore User-rated libido on a 1–5 scale, or null if unrecorded.
 * @property customTags  Freeform user tags stored as a JSON-serialized `List<String>`.
 * @property note        Free-text note, or null if unrecorded.
 * @property cyclePhase  Cycle phase name string (e.g. "MENSTRUATION"), or null if unknown.
 * @property createdAt   Timestamp when this record was first persisted (epoch ms).
 * @property updatedAt   Timestamp of the most recent modification (epoch ms).
 */
@OptIn(ExperimentalTime::class)
@Entity(
    tableName = "daily_entries"
)
data class DailyEntryEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "entry_date", index = true) val entryDate: LocalDate,
    @ColumnInfo(name = "day_in_cycle") val dayInCycle: Int,
    @ColumnInfo(name = "mood_score") val moodScore: Int? = null,
    @ColumnInfo(name = "energy_level") val energyLevel: Int? = null,
    @ColumnInfo(name = "libido_score") val libidoScore: Int? = null,
    @ColumnInfo(name = "custom_tags") val customTags: String,
    @ColumnInfo(name = "note") val note: String? = null,
    @ColumnInfo(name = "cycle_phase") val cyclePhase: String? = null,
    @ColumnInfo(name = "created_at") val createdAt: Instant,
    @ColumnInfo(name = "updated_at") val updatedAt: Instant
)
