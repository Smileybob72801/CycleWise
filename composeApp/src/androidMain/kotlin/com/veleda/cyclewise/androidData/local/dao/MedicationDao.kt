package com.veleda.cyclewise.androidData.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.veleda.cyclewise.androidData.local.entities.MedicationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MedicationDao {
    /**
     * Inserts a list of medications. If a medication with the same ID already exists,
     * it will be replaced.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedications(medications: List<MedicationEntity>)

    /**
     * Retrieves all medications associated with a single daily entry ID as a reactive Flow.
     */
    @Query("SELECT * FROM medications WHERE entry_id = :dailyEntryId")
    fun getMedicationsForEntry(dailyEntryId: String): Flow<List<MedicationEntity>>

    /**
     * Retrieves all medications for a given list of daily entry IDs.
     * This is an efficient way to fetch related data for multiple entries at once.
     */
    @Query("SELECT * FROM medications WHERE entry_id IN (:dailyEntryIds)")
    fun getMedicationsForEntries(dailyEntryIds: List<String>): Flow<List<MedicationEntity>>

    /**
     * Deletes all medications associated with a single daily entry ID. This is useful
     * for transactional updates.
     */
    @Query("DELETE FROM medications WHERE entry_id = :dailyEntryId")
    suspend fun deleteMedicationsForEntry(dailyEntryId: String)
}