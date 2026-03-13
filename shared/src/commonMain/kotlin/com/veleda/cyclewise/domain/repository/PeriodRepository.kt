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
 * Central contract for all period, daily log, library, and water intake data access.
 *
 * Platform-specific implementations (e.g., [RoomPeriodRepository]) handle persistence.
 * All [Flow]-returning methods emit cold streams — subscribers receive the current snapshot
 * on subscription and subsequent updates whenever the underlying data changes.
 * All `suspend` methods are safe to call from any dispatcher (Room handles IO internally).
 */
interface PeriodRepository {

    // ── Period CRUD ──────────────────────────────────────────────────────

    /**
     * Returns all known periods as a reactive stream, sorted by start date **descending**
     * (most recent first).
     */
    fun getAllPeriods(): Flow<List<Period>>

    /**
     * Returns a single period by its UUID.
     *
     * @throws NoSuchElementException if no period with [periodId] exists.
     */
    suspend fun getPeriodById(periodId: String): Period

    /**
     * Creates and persists a new ongoing period (endDate = null) starting on [startDate].
     *
     * @return the newly created [Period].
     */
    suspend fun startNewPeriod(startDate: LocalDate): Period

    /**
     * Updates the end date of the period identified by [periodId].
     *
     * Pass null for [endDate] to reopen the period as ongoing.
     *
     * @return the updated [Period], or null if no period with [periodId] exists.
     */
    suspend fun updatePeriodEndDate(periodId: String, endDate: LocalDate?): Period?

    /**
     * Marks the period identified by [periodId] as ended on [endDate].
     *
     * @return the updated [Period], or null if no period with [periodId] exists.
     */
    suspend fun endPeriod(periodId: String, endDate: LocalDate): Period?

    /**
     * Returns the currently ongoing period (endDate == null), or null if none exists.
     * At most one ongoing period should exist at any time.
     */
    suspend fun getCurrentlyOngoingPeriod(): Period?

    /**
     * Creates a new already-completed period with both start and end dates set.
     *
     * @return the newly created [Period].
     */
    suspend fun createCompletedPeriod(startDate: LocalDate, endDate: LocalDate): Period

    /**
     * Checks whether [startDate]..[endDate] overlaps with any existing period.
     *
     * @return true if the range is free of overlap.
     */
    suspend fun isDateRangeAvailable(startDate: LocalDate, endDate: LocalDate): Boolean

    /**
     * Permanently deletes the period identified by [id] and its associated period logs.
     * Daily entries and their symptom/medication logs are preserved.
     */
    suspend fun deletePeriod(id: String)

    // ── Period Day Marking (State Machine) ───────────────────────────────

    /**
     * Marks [date] as a period day, merging or extending adjacent periods as needed.
     *
     * Runs inside a database transaction. Handles four scenarios:
     * 1. **Already inside a period** — ensures a [PeriodLog] exists for the date.
     * 2. **Bridges two periods** — merges the day-before and day-after periods into one.
     * 3. **Extends a period** — adjusts the neighboring period's start or end date.
     * 4. **Island day** — creates a new single-day completed period.
     */
    suspend fun logPeriodDay(date: LocalDate)

    /**
     * Unmarks [date] as a period day, splitting or shrinking the containing period.
     *
     * Runs inside a database transaction. Handles four scenarios:
     * 1. **Single-day period** — deletes the entire period.
     * 2. **Start date** — advances the period's start date by one day.
     * 3. **End date** — retracts the period's end date by one day.
     * 4. **Middle day** — splits the period into two around the removed date.
     *
     * Also deletes any [PeriodLog] for the date.
     */
    suspend fun unLogPeriodDay(date: LocalDate)

    // ── Daily Log Access ─────────────────────────────────────────────────

    /**
     * Returns the full daily log for [date], or null if no entry exists for that date.
     */
    suspend fun getFullLogForDate(date: LocalDate): FullDailyLog?

    /**
     * Persists all data for a single day in a transaction.
     *
     * Uses delete-then-insert semantics for period logs, symptom logs, and medication logs:
     * existing child records for the day are removed before the new set is inserted.
     */
    suspend fun saveFullLog(log: FullDailyLog)

    /**
     * Returns all full daily logs whose entry dates fall within [yearMonth].
     */
    suspend fun getLogsForMonth(yearMonth: YearMonth): List<FullDailyLog>

    /**
     * Returns a reactive stream of all daily logs across all dates.
     * Each emission is a complete snapshot including period logs, symptom logs, and medication logs.
     */
    fun getAllLogs(): Flow<List<FullDailyLog>>

    // ── Calendar Observation ─────────────────────────────────────────────

    /**
     * Emits the set of all [LocalDate]s that fall within any saved period's date range.
     * Ongoing periods extend through today.
     */
    fun observeAllPeriodDays(): Flow<Set<LocalDate>>

    /**
     * Emits a consolidated map of [DayDetails] for every day with recorded data.
     *
     * Combines period date ranges, symptom logs, and medication logs into a single
     * map keyed by date. This is the **single source of truth** for the calendar UI.
     */
    fun observeDayDetails(): Flow<Map<LocalDate, DayDetails>>

    // ── Symptom Library ──────────────────────────────────────────────────

    /**
     * Returns a reactive stream of all symptoms in the library, sorted by name ascending.
     */
    fun getSymptomLibrary(): Flow<List<Symptom>>

    /**
     * Returns the existing symptom with [name], or creates a new one if none exists.
     *
     * Name matching is case-sensitive. The [category] parameter is only used when
     * creating a new symptom.
     *
     * @return the existing or newly created [Symptom].
     */
    suspend fun createOrGetSymptomInLibrary(name: String, category: SymptomCategory = SymptomCategory.OTHER): Symptom

    /**
     * Seeds the symptom library with a default set of common symptoms (20 entries).
     * Uses INSERT IGNORE semantics so existing symptoms are not duplicated.
     */
    suspend fun prepopulateSymptomLibrary()

    /** Returns a one-shot list of all [SymptomLog] entries across all dates. */
    suspend fun getAllSymptomLogs(): List<SymptomLog>

    /**
     * Renames a symptom in the library.
     *
     * @param symptomId UUID of the symptom to rename.
     * @param newName   the new unique name.
     */
    suspend fun renameSymptom(symptomId: String, newName: String)

    /**
     * Deletes a symptom from the library. CASCADE removes all associated symptom logs.
     *
     * @param symptomId UUID of the symptom to delete.
     */
    suspend fun deleteSymptom(symptomId: String)

    /**
     * Returns the number of daily log entries that reference the given symptom.
     *
     * @param symptomId UUID of the symptom.
     * @return count of symptom log entries.
     */
    suspend fun getSymptomLogCount(symptomId: String): Int

    // ── Medication Library ───────────────────────────────────────────────

    /**
     * Returns a reactive stream of all medications in the library, sorted by name ascending.
     */
    fun getMedicationLibrary(): Flow<List<Medication>>

    /**
     * Returns the existing medication with [name], or creates a new one if none exists.
     *
     * @return the existing or newly created [Medication].
     */
    suspend fun createOrGetMedicationInLibrary(name: String): Medication

    /**
     * Seeds the medication library with a default set of common medications (12 entries).
     * Uses INSERT IGNORE semantics so existing medications are not duplicated.
     */
    suspend fun prepopulateMedicationLibrary()

    /** Returns a one-shot list of all [MedicationLog] entries across all dates. */
    suspend fun getAllMedicationLogs(): List<MedicationLog>

    /**
     * Renames a medication in the library.
     *
     * @param medicationId UUID of the medication to rename.
     * @param newName      the new unique name.
     */
    suspend fun renameMedication(medicationId: String, newName: String)

    /**
     * Deletes a medication from the library. CASCADE removes all associated medication logs.
     *
     * @param medicationId UUID of the medication to delete.
     */
    suspend fun deleteMedication(medicationId: String)

    /**
     * Returns the number of daily log entries that reference the given medication.
     *
     * @param medicationId UUID of the medication.
     * @return count of medication log entries.
     */
    suspend fun getMedicationLogCount(medicationId: String): Int

    // ── Water Intake ─────────────────────────────────────────────────────

    /**
     * Inserts or replaces the water intake record for [intake]'s date (upsert semantics).
     */
    suspend fun upsertWaterIntake(intake: WaterIntake)

    /**
     * Returns water intake records for the specified [dates].
     * Dates with no record are omitted from the result.
     */
    suspend fun getWaterIntakeForDates(dates: List<LocalDate>): List<WaterIntake>

    /**
     * Returns a reactive stream of all water intake records across all dates.
     */
    fun getAllWaterIntakes(): Flow<List<WaterIntake>>

    // ── Tutorial Cleanup ─────────────────────────────────────────────────

    /**
     * Deletes tutorial seed data identified by exact IDs.
     *
     * Runs inside a database transaction. CASCADE delete on daily entries
     * automatically removes associated period_logs, symptom_logs, and medication_logs.
     *
     * @param periodUuids UUIDs of seeded periods to delete.
     * @param entryIds    IDs of seeded daily entries to delete.
     * @param waterDates  ISO-8601 dates of seeded water intake records to delete.
     */
    suspend fun deleteSeedData(
        periodUuids: List<String>,
        entryIds: List<String>,
        waterDates: List<LocalDate>,
    )

    // ── Debug ────────────────────────────────────────────────────────────

    /**
     * **[DEBUG ONLY]** Deletes all user data and seeds the database with several months
     * of generated cycles, daily logs, symptoms, and medications.
     *
     * **Destructive:** all existing data is permanently lost.
     */
    suspend fun seedDatabaseForDebug()
}
