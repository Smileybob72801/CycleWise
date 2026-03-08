package com.veleda.cyclewise.androidData.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.veleda.cyclewise.androidData.local.entities.MedicationLogEntity
import kotlinx.coroutines.flow.Flow

/**
 * Room DAO for the `medication_logs` table.
 *
 * Each row records that a medication was taken on a single day. Linked to `daily_entries`
 * via `entry_id` (CASCADE delete) and to `medication_library` via `medication_id` (RESTRICT delete).
 */
@Dao
interface MedicationLogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(logs: List<MedicationLogEntity>)

    @Query("SELECT * FROM medication_logs WHERE entry_id = :dailyEntryId")
    fun getLogsForEntry(dailyEntryId: String): Flow<List<MedicationLogEntity>>

    @Query("SELECT * FROM medication_logs WHERE entry_id IN (:dailyEntryIds)")
    fun getLogsForEntries(dailyEntryIds: List<String>): Flow<List<MedicationLogEntity>>

    @Query("DELETE FROM medication_logs WHERE entry_id = :dailyEntryId")
    suspend fun deleteLogsForEntry(dailyEntryId: String)

    @Query("SELECT * FROM medication_logs")
    fun getAllMedicationLogs(): Flow<List<MedicationLogEntity>>

    @Query("DELETE FROM medication_logs")
    suspend fun deleteAll()
}