package com.veleda.cyclewise.androidData.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.veleda.cyclewise.androidData.local.entities.SymptomEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SymptomDao {
    /**
     * Inserts a list of symptoms. If a symptom with the same ID already exists,
     * it will be replaced.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSymptoms(symptoms: List<SymptomEntity>)

    /**
     * Retrieves all symptoms associated with a single daily entry ID as a reactive Flow.
     */
    @Query("SELECT * FROM symptoms WHERE entry_id = :dailyEntryId")
    fun getSymptomsForEntry(dailyEntryId: String): Flow<List<SymptomEntity>>

    /**
     * Retrieves all symptoms for a given list of daily entry IDs.
     * This is an efficient way to fetch related data for multiple entries at once.
     */
    @Query("SELECT * FROM symptoms WHERE entry_id IN (:dailyEntryIds)")
    fun getSymptomsForEntries(dailyEntryIds: List<String>): Flow<List<SymptomEntity>>

    /**
     * Deletes all symptoms associated with a single daily entry ID. This is useful
     * for transactional updates.
     */
    @Query("DELETE FROM symptoms WHERE entry_id = :dailyEntryId")
    suspend fun deleteSymptomsForEntry(dailyEntryId: String)
}
