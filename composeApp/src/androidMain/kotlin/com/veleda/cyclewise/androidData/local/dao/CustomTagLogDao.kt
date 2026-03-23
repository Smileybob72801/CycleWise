package com.veleda.cyclewise.androidData.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.veleda.cyclewise.androidData.local.entities.CustomTagLogEntity
import kotlinx.coroutines.flow.Flow

/**
 * Room DAO for the `custom_tag_logs` table.
 *
 * Each row records that a custom tag was applied to a single day. Linked to `daily_entries`
 * via `entry_id` (CASCADE delete) and to `custom_tag_library` via `tag_id` (CASCADE delete).
 */
@Dao
interface CustomTagLogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(logs: List<CustomTagLogEntity>)

    @Query("SELECT * FROM custom_tag_logs WHERE entry_id = :dailyEntryId")
    fun getLogsForEntry(dailyEntryId: String): Flow<List<CustomTagLogEntity>>

    @Query("SELECT * FROM custom_tag_logs WHERE entry_id IN (:dailyEntryIds)")
    fun getLogsForEntries(dailyEntryIds: List<String>): Flow<List<CustomTagLogEntity>>

    @Query("DELETE FROM custom_tag_logs WHERE entry_id = :dailyEntryId")
    suspend fun deleteLogsForEntry(dailyEntryId: String)

    @Query("SELECT * FROM custom_tag_logs")
    fun getAllCustomTagLogs(): Flow<List<CustomTagLogEntity>>

    @Query("DELETE FROM custom_tag_logs")
    suspend fun deleteAll()
}
