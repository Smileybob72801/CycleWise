package com.veleda.cyclewise.androidData.local.dao

import androidx.room.*
import com.veleda.cyclewise.androidData.local.entities.SymptomLogEntity
import kotlinx.coroutines.flow.Flow

/**
 * Room DAO for the `symptom_logs` table.
 *
 * Each row records a symptom occurrence for a single day. Linked to `daily_entries`
 * via `entry_id` (CASCADE delete) and to `symptom_library` via `symptom_id` (RESTRICT delete).
 */
@Dao
interface SymptomLogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(logs: List<SymptomLogEntity>)

    @Query("SELECT * FROM symptom_logs WHERE entry_id = :dailyEntryId")
    fun getLogsForEntry(dailyEntryId: String): Flow<List<SymptomLogEntity>>

    @Query("SELECT * FROM symptom_logs WHERE entry_id IN (:dailyEntryIds)")
    fun getLogsForEntries(dailyEntryIds: List<String>): Flow<List<SymptomLogEntity>>

    @Query("DELETE FROM symptom_logs WHERE entry_id = :dailyEntryId")
    suspend fun deleteLogsForEntry(dailyEntryId: String)

    @Query("SELECT * FROM symptom_logs")
    fun getAllSymptomLogs(): Flow<List<SymptomLogEntity>>

    @Query("DELETE FROM symptom_logs")
    suspend fun deleteAll()
}