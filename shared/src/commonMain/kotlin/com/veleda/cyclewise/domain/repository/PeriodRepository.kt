package com.veleda.cyclewise.domain.repository

import com.veleda.cyclewise.domain.models.Period
import com.veleda.cyclewise.domain.models.DayDetails
import com.veleda.cyclewise.domain.models.FullDailyLog
import com.veleda.cyclewise.domain.models.Medication
import com.veleda.cyclewise.domain.models.MedicationLog
import com.veleda.cyclewise.domain.models.Symptom
import com.veleda.cyclewise.domain.models.SymptomCategory
import com.veleda.cyclewise.domain.models.SymptomLog
import com.veleda.cyclewise.domain.models.WaterIntake
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate
import kotlinx.datetime.YearMonth

/**
 * Shared interface for accessing and modifying period data.
 * Platform-specific implementations will handle persistence.
 */
interface PeriodRepository {

    /** Returns all known periods, sorted by start date descending. */
    fun getAllPeriods(): Flow<List<Period>>

    /** Returns a single period given its primary Id. */
    suspend fun getPeriodById(periodId: String): Period

    /** Starts a new period on the given start date. */
    suspend fun startNewPeriod(startDate: LocalDate): Period

    /** Updates the end date of an existing period. Can be null to make it ongoing. */
    suspend fun updatePeriodEndDate(periodId: String, endDate: LocalDate?): Period?

    /** Marks an existing period as ended on the given date. */
    suspend fun endPeriod(periodId: String, endDate: LocalDate): Period?

    /** Returns the currently active (non-ended) period, if any. */
    suspend fun getCurrentlyOngoingPeriod(): Period?

    /** Returns the full daily log for a specific date, or null if none exists. */
    suspend fun getFullLogForDate(date: LocalDate): FullDailyLog?

    /** Store all data for a day. */
    suspend fun saveFullLog(log: FullDailyLog)

    /** Fetches all full daily logs for a given month. */
    suspend fun getLogsForMonth(yearMonth: YearMonth): List<FullDailyLog>

    /** Creates a new, already-completed period in the database. */
    suspend fun createCompletedPeriod(startDate: LocalDate, endDate: LocalDate): Period

    /** Checks if a given date range overlaps with any existing periods. */
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
     * Observes all underlying data sources (Periods, logs, etc.) and emits a consolidated
     * map of details for each relevant day, using a UI-agnostic domain model.
     * This is the single source of truth for the calendar UI.
     */
    fun observeDayDetails(): Flow<Map<LocalDate, DayDetails>>

    /** Fetches a one-shot list of all symptom logs. */
    suspend fun getAllSymptomLogs(): List<SymptomLog>

    /** Fetches a one-shot list of all medication logs. */
    suspend fun getAllMedicationLogs(): List<MedicationLog>

    /** [DEBUG ONLY] Clears and seeds the database with test data. */
    suspend fun seedDatabaseForDebug()

    /** Permanently deletes a period. */
    suspend fun deletePeriod(id: String)

    /** Marks a specific date as a period day, merging or extending adjacent periods if necessary. */
    suspend fun logPeriodDay(date: LocalDate)

    /** Unmarks a specific date as a period day, splitting the existing period or deleting a 1-day period. */
    suspend fun unLogPeriodDay(date: LocalDate)

    /** Inserts or replaces a water intake record for a given date. */
    suspend fun upsertWaterIntake(intake: WaterIntake)

    /** Returns water intake records for the specified dates. */
    suspend fun getWaterIntakeForDates(dates: List<LocalDate>): List<WaterIntake>
}
