package com.veleda.cyclewise.androidData.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.veleda.cyclewise.androidData.local.entities.MedicationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MedicationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedications(medications: List<MedicationEntity>)

    @Query("SELECT * FROM medications WHERE entry_id = :dailyEntryId")
    fun getMedicationsForEntry(dailyEntryId: String): Flow<List<MedicationEntity>>

    @Query("DELETE FROM medications WHERE entry_id = :dailyEntryId")
    suspend fun deleteMedicationsForEntry(dailyEntryId: String)
}