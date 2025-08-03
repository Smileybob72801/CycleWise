package com.veleda.cyclewise.androidData.local.dao

import androidx.room.*
import com.veleda.cyclewise.androidData.local.entities.CycleEntity
import kotlinx.coroutines.flow.Flow
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
@Dao
interface CycleDao {
    /** All cycles, newest first (by start_date). */
    @Query("SELECT * FROM cycles ORDER BY start_date DESC")
    fun getAllCycles(): Flow<List<CycleEntity>>

    /** Lookup by internal PK (rarely needed outside Room). */
    @Query("SELECT * FROM cycles WHERE id = :internalId")
    suspend fun getById(internalId: Int): CycleEntity?

    /** Lookup by exposed UUID. */
    @Query("SELECT * FROM cycles WHERE uuid = :uuid")
    suspend fun getByUuid(uuid: String): CycleEntity?

    /** Insert new cycle; uuid must be set on the entity. */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(cycle: CycleEntity)

    /** Update an existing cycle (e.g. to set endDate). */
    @Update
    suspend fun update(cycle: CycleEntity)

    /** Reactive stream of currently ongoing cycle (end_date IS NULL). */
    @Query("SELECT * FROM cycles WHERE end_date IS NULL LIMIT 1")
    fun getOngoingCycle(): Flow<CycleEntity?>
}