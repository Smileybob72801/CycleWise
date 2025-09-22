package com.veleda.cyclewise.domain.repository

import com.veleda.cyclewise.domain.models.Cycle
import com.veleda.cyclewise.domain.models.DailyEntry
import com.veleda.cyclewise.domain.models.DayDetails
import com.veleda.cyclewise.domain.models.FullDailyLog
import com.veleda.cyclewise.domain.models.Medication
import com.veleda.cyclewise.domain.models.Symptom
import com.veleda.cyclewise.domain.models.SymptomCategory
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate
import kotlinx.datetime.YearMonth

/**
 * Shared interface for accessing and modifying cycle data.
 * Platform-specific implementations will handle persistence.
 */
interface CycleRepository {

    /** Returns all known cycles, sorted by start date descending. */
    fun getAllCycles(): Flow<List<Cycle>>

    /** Returns a single cycle given its primary Id. */
    suspend fun getCycleById(cycleId: String): Cycle

    /** Starts a new cycle on the given start date. */
    suspend fun startNewCycle(startDate: LocalDate) : Cycle

    /** Updates the end date of an existing cycle. Can be null to make it ongoing. */
    suspend fun updateCycleEndDate(cycleId: String, endDate: LocalDate?): Cycle?

    /** Marks an existing cycle as ended on the given date. */
    suspend fun endCycle(cycleId: String, endDate: LocalDate): Cycle?

    /** Returns the currently active (non-ended) cycle, if any. */
    suspend fun getCurrentlyOngoingCycle(): Cycle?

    /** Fetches all daily logs for a given month. */
    suspend fun getFullLogForDate(date: LocalDate): FullDailyLog?

    /** Store all data for a day. */
    suspend fun saveFullLog(log: FullDailyLog)

    /** Returns all data for a day. */
    suspend fun getLogsForMonth(yearMonth: YearMonth): List<FullDailyLog>

    /** Creates a new, already-completed cycle in the database. */
    suspend fun createCompletedCycle(startDate: LocalDate, endDate: LocalDate): Cycle

    /** Checks if a given date range overlaps with any existing cycles. */
    suspend fun isDateRangeAvailable(startDate: LocalDate, endDate: LocalDate): Boolean

    /** Returns a reactive Flow of all unique medications in the user's library. */
    fun getMedicationLibrary(): Flow<List<Medication>>

    /**
     * Creates a new unique medication in the library if it doesn't already exist by name,
     * then returns the Medication object (either the new one or the existing one).
     */
    suspend fun createOrGetMedicationInLibrary(name: String): Medication

    /** Returns a reactive Flow of all unique symptoms in the user's library. */
    fun getSymptomLibrary(): Flow<List<Symptom>>

    /**
     * Creates a new unique symptom in the library if it doesn't already exist by name,
     * then returns the Symptom object (either the new one or the existing one).
     */
    suspend fun createOrGetSymptomInLibrary(name: String, category: SymptomCategory = SymptomCategory.OTHER): Symptom

    /** Pre-populates the symptom library with a default set of symptoms. */
    suspend fun prepopulateSymptomLibrary()

    /** Returns a reactive Flow of ALL daily logs that have been created. */
    fun getAllLogs(): Flow<List<FullDailyLog>>

    /** Emits the set of all LocalDates that are part of any saved menstrual period. */
    fun observeAllPeriodDays(): Flow<Set<LocalDate>>

    /**
     * Observes all underlying data sources (cycles, logs, etc.) and emits a consolidated
     * map of details for each relevant day, using a UI-agnostic domain model.
     * This is the single source of truth for the calendar UI.
     */
    fun observeDayDetails(): Flow<Map<LocalDate, DayDetails>>
}
