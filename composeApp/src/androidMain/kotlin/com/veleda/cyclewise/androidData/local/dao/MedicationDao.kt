package com.veleda.cyclewise.androidData.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.veleda.cyclewise.androidData.local.entities.MedicationEntity
import com.veleda.cyclewise.androidData.local.entities.MedicationLogEntity
import kotlinx.coroutines.flow.Flow

/**
 * Room DAO for the `medication_library` table.
 *
 * Manages the user's unique medication types. Names are unique (IGNORE on conflict).
 * Results from [getAllMedications] are sorted by name ascending.
 */
@Dao
interface MedicationDao {
    @Query("SELECT * FROM medication_library ORDER BY name ASC")
    fun getAllMedications(): Flow<List<MedicationEntity>>

    @Query("SELECT * FROM medication_library WHERE name = :name LIMIT 1")
    suspend fun getMedicationByName(name: String): MedicationEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(medication: MedicationEntity)
}