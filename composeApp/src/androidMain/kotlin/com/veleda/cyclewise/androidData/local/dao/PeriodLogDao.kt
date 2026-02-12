package com.veleda.cyclewise.androidData.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.veleda.cyclewise.androidData.local.entities.PeriodLogEntity
import kotlinx.coroutines.flow.Flow

/**
 * Room DAO for the `period_logs` table.
 *
 * Each row records the flow intensity for a single day, linked to a [DailyEntryEntity]
 * via `entry_id` FK (CASCADE delete). At most one period log exists per daily entry.
 */
@Dao
interface PeriodLogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: PeriodLogEntity)

    @Update
    suspend fun update(log: PeriodLogEntity)

    @Query("SELECT * FROM period_logs WHERE entry_id = :dailyEntryId LIMIT 1")
    fun getLogForEntry(dailyEntryId: String): Flow<PeriodLogEntity?>

    @Query("DELETE FROM period_logs WHERE entry_id = :dailyEntryId")
    suspend fun deleteLogForEntry(dailyEntryId: String)

    @Query("SELECT * FROM period_logs")
    fun getAllPeriodLogs(): Flow<List<PeriodLogEntity>>

    @Query("DELETE FROM period_logs")
    suspend fun deleteAll()
}