package com.veleda.cyclewise.androidData.local.dao

import androidx.room.*
import com.veleda.cyclewise.androidData.local.entities.PeriodEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
@Dao
interface PeriodDao {
    /** All periods, newest first (by start_date). */
    @Query("SELECT * FROM periods ORDER BY start_date DESC")
    fun getAllPeriods(): Flow<List<PeriodEntity>>

    /** Lookup by internal PK (rarely needed outside Room). */
    @Query("SELECT * FROM periods WHERE id = :internalId")
    suspend fun getById(internalId: Int): PeriodEntity?

    /** Lookup by exposed UUID. */
    @Query("SELECT * FROM periods WHERE uuid = :uuid")
    suspend fun getByUuid(uuid: String): PeriodEntity?

    /** Insert new period; uuid must be set on the entity. */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(period: PeriodEntity)

    /** Update an existing period (e.g. to set endDate). */
    @Update
    suspend fun update(period: PeriodEntity)

    /** Reactive stream of currently ongoing cycle (end_date IS NULL). */
    @Query("SELECT * FROM periods WHERE end_date IS NULL LIMIT 1")
    fun getOngoingPeriod(): Flow<PeriodEntity?>

    /**
     * Counts periods that overlap with the given date range, excluding a specific cycle (for updates).
     * The logic is: (new_start <= existing_end) AND (new_end >= existing_start)
     */
    @Query("""
        SELECT COUNT(id) FROM periods
        WHERE (:startDate <= end_date OR end_date IS NULL) 
        AND (:endDate >= start_date)
    """)
    suspend fun getOverlappingPeriodsCount(startDate: LocalDate, endDate: LocalDate): Int

    @Query("DELETE FROM periods")
    suspend fun deleteAll()

    /** Delete period by exposed UUID. */
    @Query("DELETE FROM periods WHERE uuid = :uuid")
    suspend fun deleteByUuid(uuid: String)
}