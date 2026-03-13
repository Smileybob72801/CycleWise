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

    /** Renames a medication in the library by its UUID. */
    @Query("UPDATE medication_library SET name = :newName WHERE id = :id")
    suspend fun updateName(id: String, newName: String)

    /** Deletes a medication from the library by its UUID. CASCADE removes associated logs. */
    @Query("DELETE FROM medication_library WHERE id = :id")
    suspend fun deleteById(id: String)

    /** Returns the number of medication log entries that reference the given [medicationId]. */
    @Query("SELECT COUNT(*) FROM medication_logs WHERE medication_id = :medicationId")
    suspend fun countLogsForMedication(medicationId: String): Int
}