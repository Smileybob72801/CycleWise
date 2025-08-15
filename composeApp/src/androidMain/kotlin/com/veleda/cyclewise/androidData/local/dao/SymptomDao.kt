package com.veleda.cyclewise.androidData.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.veleda.cyclewise.androidData.local.entities.SymptomEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SymptomDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSymptoms(symptoms: List<SymptomEntity>)

    @Query("SELECT * FROM symptoms WHERE entry_id = :dailyEntryId")
    fun getSymptomsForEntry(dailyEntryId: String): Flow<List<SymptomEntity>>

    @Query("DELETE FROM symptoms WHERE entry_id = :dailyEntryId")
    suspend fun deleteSymptomsForEntry(dailyEntryId: String)
}