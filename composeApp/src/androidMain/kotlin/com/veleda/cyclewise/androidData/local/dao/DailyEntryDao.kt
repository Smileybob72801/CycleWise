package com.veleda.cyclewise.androidData.local.dao

import androidx.room.*
import com.veleda.cyclewise.androidData.local.entities.DailyEntryEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

@Dao
interface DailyEntryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: DailyEntryEntity)

    @Update
    suspend fun update(entry: DailyEntryEntity)

    @Query("SELECT * FROM daily_entries WHERE entry_date = :date LIMIT 1")
    fun getEntryForDate(date: LocalDate): Flow<DailyEntryEntity?>

    @Query("SELECT * FROM daily_entries WHERE cycle_id = :cycleId ORDER BY entry_date ASC")
    fun getEntriesForCycle(cycleId: String): Flow<List<DailyEntryEntity>>
}
