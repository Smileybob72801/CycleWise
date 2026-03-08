package com.veleda.cyclewise.androidData.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.veleda.cyclewise.androidData.local.entities.SymptomEntity
import kotlinx.coroutines.flow.Flow

/**
 * Room DAO for the `symptom_library` table.
 *
 * Manages the user's unique symptom types. Names are unique (IGNORE on conflict).
 * Results from [getAllSymptoms] are sorted by name ascending.
 */
@Dao
interface SymptomDao {
    @Query("SELECT * FROM symptom_library ORDER BY name ASC")
    fun getAllSymptoms(): Flow<List<SymptomEntity>>

    @Query("SELECT * FROM symptom_library WHERE name = :name LIMIT 1")
    suspend fun getSymptomByName(name: String): SymptomEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(symptom: SymptomEntity)
}