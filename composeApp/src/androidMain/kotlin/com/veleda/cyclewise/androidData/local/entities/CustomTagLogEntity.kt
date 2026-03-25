package com.veleda.cyclewise.androidData.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import kotlin.time.Instant

/**
 * Room entity for the `custom_tag_logs` table.
 *
 * Records that a custom tag was applied to a single day. FK to `daily_entries` (CASCADE)
 * and to `custom_tag_library` (CASCADE — deleting a tag from the library automatically
 * removes all its log entries).
 */
@Entity(
    tableName = "custom_tag_logs",
    foreignKeys = [
        ForeignKey(
            entity = DailyEntryEntity::class,
            parentColumns = ["id"],
            childColumns = ["entry_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = CustomTagEntity::class,
            parentColumns = ["id"],
            childColumns = ["tag_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class CustomTagLogEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "entry_id", index = true) val entryId: String,
    @ColumnInfo(name = "tag_id", index = true) val tagId: String,
    @ColumnInfo(name = "created_at") val createdAt: Instant
)
