package com.veleda.cyclewise.androidData.repository

import androidx.room.withTransaction
import com.benasher44.uuid.uuid4
import com.veleda.cyclewise.androidData.local.dao.PeriodDao
import com.veleda.cyclewise.androidData.local.dao.DailyEntryDao
import com.veleda.cyclewise.androidData.local.dao.MedicationDao
import com.veleda.cyclewise.androidData.local.dao.MedicationLogDao
import com.veleda.cyclewise.androidData.local.dao.PeriodLogDao
import com.veleda.cyclewise.androidData.local.dao.SymptomDao
import com.veleda.cyclewise.androidData.local.dao.SymptomLogDao
import com.veleda.cyclewise.androidData.local.database.PeriodDatabase
import com.veleda.cyclewise.androidData.local.entities.toDomain
import com.veleda.cyclewise.androidData.local.entities.toEntity
import com.veleda.cyclewise.domain.models.Period
import com.veleda.cyclewise.domain.models.DailyEntry
import com.veleda.cyclewise.domain.models.FlowIntensity
import com.veleda.cyclewise.domain.models.MedicationLog
import com.veleda.cyclewise.domain.models.SymptomLog
import com.veleda.cyclewise.domain.models.FullDailyLog
import com.veleda.cyclewise.domain.models.Medication
import com.veleda.cyclewise.domain.models.Symptom
import com.veleda.cyclewise.domain.models.SymptomCategory
import com.veleda.cyclewise.domain.repository.PeriodRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.datetime.*
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import com.veleda.cyclewise.domain.models.DayDetails
import com.veleda.cyclewise.domain.models.PeriodLog
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DateTimeUnit.Companion.DAY
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import kotlin.random.Random
import com.veleda.cyclewise.androidData.local.entities.PeriodEntity

class RoomPeriodRepository(
    private val db: PeriodDatabase,
    private val periodDao: PeriodDao,
    private val dailyEntryDao: DailyEntryDao,
    private val symptomDao: SymptomDao,
    private val medicationDao: MedicationDao,
    private val medicationLogDao: MedicationLogDao,
    private val symptomLogDao: SymptomLogDao,
    private val periodLogDao: PeriodLogDao,
) : PeriodRepository {
    override fun getAllPeriods(): Flow<List<Period>> {
        return periodDao.getAllPeriods().map { entityList ->
            entityList.map { it.toDomain() }
        }
    }

    override suspend fun getPeriodById(periodId: String): Period {
        val entity = periodDao.getByUuid(periodId)
            ?: throw NoSuchElementException("Period $periodId not found")
        return entity.toDomain()
    }

    @OptIn(ExperimentalTime::class)
    override suspend fun startNewPeriod(startDate: LocalDate): Period {
        val now = Clock.System.now()
        val cycleUuid  = uuid4().toString()

        val domainPeriod = Period(
            id = cycleUuid ,
            startDate = startDate,
            endDate = null,
            createdAt = now,
            updatedAt = now
        )

        periodDao.insert(domainPeriod.toEntity())

        return domainPeriod
    }

    override suspend fun endPeriod(periodId: String, endDate: LocalDate): Period? {
        val existing = periodDao.getByUuid(periodId) ?: return null
        val updated = existing.copy(endDate = endDate)
        periodDao.update(updated)
        return updated.toDomain()
    }

    override suspend fun getCurrentlyOngoingPeriod(): Period? {
        return periodDao.getOngoingPeriod().firstOrNull()?.toDomain()
    }

    override suspend fun getFullLogForDate(date: LocalDate): FullDailyLog? {
        val entryEntity = dailyEntryDao.getEntryForDate(date).firstOrNull() ?: return null
        val periodLog = periodLogDao.getLogForEntry(entryEntity.id).firstOrNull()
        val symptomLogs = symptomLogDao.getLogsForEntry(entryEntity.id).firstOrNull() ?: emptyList()
        val medicationLogs = medicationLogDao.getLogsForEntry(entryEntity.id).firstOrNull() ?: emptyList()
        return FullDailyLog(
            entry = entryEntity.toDomain(),
            periodLog = periodLog?.toDomain(),
            symptomLogs = symptomLogs.map { it.toDomain() },
            medicationLogs = medicationLogs.map { it.toDomain() }
        )
    }

    override suspend fun saveFullLog(log: FullDailyLog) {
        db.withTransaction {
            // 1. Always save DailyEntry first
            dailyEntryDao.insert(log.entry.toEntity())

            // 2. Handle PeriodLog (Flow data)
            periodLogDao.deleteLogForEntry(log.entry.id)
            log.periodLog?.let { periodLog ->
                periodLogDao.insert(periodLog.toEntity())
            }

            // 3. Handle Symptom Logs
            symptomLogDao.deleteLogsForEntry(log.entry.id)
            if (log.symptomLogs.isNotEmpty()) {
                symptomLogDao.insertAll(log.symptomLogs.map { it.toEntity() })
            }

            // 4. Handle Medication Logs
            medicationLogDao.deleteLogsForEntry(log.entry.id)
            if (log.medicationLogs.isNotEmpty()) {
                medicationLogDao.insertAll(log.medicationLogs.map { it.toEntity() })
            }
        }
    }

    override suspend fun getLogsForMonth(yearMonth: YearMonth): List<FullDailyLog> {
        val startDate = yearMonth.firstDay
        val endDate = yearMonth.lastDay

        val entryEntities = dailyEntryDao.getEntriesForDateRange(startDate, endDate).firstOrNull() ?: return emptyList()
        if (entryEntities.isEmpty()) return emptyList()

        val entryIds = entryEntities.map { it.id }
        val allSymptomLogs = symptomLogDao.getLogsForEntries(entryIds).firstOrNull()?.groupBy { it.entryId } ?: emptyMap()
        val allMedicationLogs = medicationLogDao.getLogsForEntries(entryIds).firstOrNull()?.groupBy { it.entryId } ?: emptyMap()

        return entryEntities.map { entryEntity ->
            FullDailyLog(
                entry = entryEntity.toDomain(),
                symptomLogs = allSymptomLogs[entryEntity.id]?.map { it.toDomain() } ?: emptyList(),
                medicationLogs = allMedicationLogs[entryEntity.id]?.map { it.toDomain() } ?: emptyList()
            )
        }
    }

    @OptIn(ExperimentalTime::class)
    override suspend fun createCompletedPeriod(startDate: LocalDate, endDate: LocalDate): Period {
        val now = Clock.System.now()
        val domainPeriod = Period(
            id = uuid4().toString(),
            startDate = startDate,
            endDate = endDate, // The end date is provided directly
            createdAt = now,
            updatedAt = now
        )
        periodDao.insert(domainPeriod.toEntity())
        return domainPeriod
    }

    override suspend fun isDateRangeAvailable(startDate: LocalDate, endDate: LocalDate): Boolean {
        val count = periodDao.getOverlappingPeriodsCount(startDate, endDate)
        return count == 0
    }

    // Helper to find a period that encompasses a date (needed for log/unlog logic)
    private suspend fun getPeriodForDate(date: LocalDate): Period? {
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        return periodDao.getAllPeriods().firstOrNull()?.find { date in (it.startDate..(it.endDate ?: today)) }?.toDomain()
    }

    // Helper to adjust period start date
    private suspend fun updatePeriodStartDate(periodId: String, newStartDate: LocalDate): Period? {
        val existing = periodDao.getByUuid(periodId) ?: return null
        val updated = existing.copy(startDate = newStartDate, updatedAt = Clock.System.now())
        periodDao.update(updated)
        return updated.toDomain()
    }

    // Helper to adjust period end date
    override suspend fun updatePeriodEndDate(periodId: String, newEndDate: LocalDate?): Period? {
        val existing = periodDao.getByUuid(periodId) ?: return null
        val updated = existing.copy(endDate = newEndDate, updatedAt = Clock.System.now())
        periodDao.update(updated)
        return updated.toDomain()
    }

    override fun getMedicationLibrary(): Flow<List<Medication>> {
        val entityFlow = medicationDao.getAllMedications()
        return entityFlow.map { entityList ->
            entityList.map { entity ->
                entity.toDomain()
            }
        }
    }

    @OptIn(ExperimentalTime::class)
    override suspend fun createOrGetMedicationInLibrary(name: String): Medication {
        val existing = medicationDao.getMedicationByName(name)
        if (existing != null) {
            return existing.toDomain()
        }
        val newMed = Medication(
            id = uuid4().toString(),
            name = name,
            createdAt = Clock.System.now()
        )
        medicationDao.insert(newMed.toEntity())
        return newMed
    }

    override fun getSymptomLibrary(): Flow<List<Symptom>> {
        return symptomDao.getAllSymptoms().map { list ->
            list.map { it.toDomain() }
        }
    }

    @OptIn(ExperimentalTime::class)
    override suspend fun createOrGetSymptomInLibrary(name: String, category: SymptomCategory): Symptom {
        val existing = symptomDao.getSymptomByName(name)
        if (existing != null) {
            return existing.toDomain()
        }
        val newSymptom = Symptom(
            id = uuid4().toString(),
            name = name,
            category = category,
            createdAt = Clock.System.now()
        )
        symptomDao.insert(newSymptom.toEntity())
        return newSymptom
    }

    @OptIn(ExperimentalTime::class)
    override suspend fun prepopulateSymptomLibrary() {
        val now = Clock.System.now()
        val defaultSymptoms = listOf(
            Symptom(uuid4().toString(), "Cramps", SymptomCategory.PAIN, now),
            Symptom(uuid4().toString(), "Headache", SymptomCategory.PAIN, now),
            Symptom(uuid4().toString(), "Joint Pain", SymptomCategory.PAIN, now),
            Symptom(uuid4().toString(), "Back Pain", SymptomCategory.PAIN, now),
            Symptom(uuid4().toString(), "Breast Tenderness", SymptomCategory.PAIN, now),
            Symptom(uuid4().toString(), "Fatigue", SymptomCategory.ENERGY, now),
            Symptom(uuid4().toString(), "Brain Fog", SymptomCategory.ENERGY, now),
            Symptom(uuid4().toString(), "Insomnia", SymptomCategory.ENERGY, now),
            Symptom(uuid4().toString(), "Dizziness", SymptomCategory.ENERGY, now),
            Symptom(uuid4().toString(), "Acne", SymptomCategory.SKIN, now),
            Symptom(uuid4().toString(), "Anxiety", SymptomCategory.MOOD, now),
            Symptom(uuid4().toString(), "Sadness", SymptomCategory.MOOD, now),
            Symptom(uuid4().toString(), "Mood Swings", SymptomCategory.MOOD, now),
            Symptom(uuid4().toString(), "Irritability", SymptomCategory.MOOD, now),
            Symptom(uuid4().toString(), "Constipation", SymptomCategory.DIGESTIVE, now),
            Symptom(uuid4().toString(), "Nausea", SymptomCategory.DIGESTIVE, now),
            Symptom(uuid4().toString(), "Diarrhea", SymptomCategory.DIGESTIVE, now),
            Symptom(uuid4().toString(), "Bloating", SymptomCategory.DIGESTIVE, now),
            Symptom(uuid4().toString(), "Increased Appetite", SymptomCategory.DIGESTIVE, now),
            Symptom(uuid4().toString(), "Decreased Appetite", SymptomCategory.DIGESTIVE, now),
            // TODO: Add additional Symptoms
        )

        // We can't use insertAll because we need onConflict=IGNORE.
        // Looping is fine for this one-time operation.
        defaultSymptoms.forEach { symptom ->
            symptomDao.insert(symptom.toEntity())
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getAllLogs(): Flow<List<FullDailyLog>> {
        // This is a complex reactive query.
        // It listens for changes in any of the log tables.
        return dailyEntryDao.getAllEntries().flatMapLatest { entries ->
            if (entries.isEmpty()) {
                flowOf(emptyList())
            } else {
                val entryIds = entries.map { it.id }
                // Combine the flows for all log types
                combine(
                    periodLogDao.getAllPeriodLogs().map { it.groupBy { log -> log.entryId } },
                    symptomLogDao.getLogsForEntries(entryIds),
                    medicationLogDao.getLogsForEntries(entryIds)
                ) { periods, symptoms, medications ->
                    val symptomsById = symptoms.groupBy { it.entryId }
                    val medicationsById = medications.groupBy { it.entryId }

                    entries.map { entry ->
                        FullDailyLog(
                            entry = entry.toDomain(),
                            periodLog = periods[entry.id]?.firstOrNull()?.toDomain(),
                            symptomLogs = symptomsById[entry.id]?.map { it.toDomain() } ?: emptyList(),
                            medicationLogs = medicationsById[entry.id]?.map { it.toDomain() } ?: emptyList()
                        )
                    }
                }
            }
        }
    }

    override fun observeAllPeriodDays(): Flow<Set<LocalDate>> {
        // This will automatically re-run whenever the 'periods' table changes.
        return periodDao.getAllPeriods().map { cycles ->
            // Use buildSet for an efficient way to create the set of dates.
            buildSet {
                val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
                for (cycle in cycles) {
                    val startDate = cycle.startDate
                    val endDate = cycle.endDate ?: today

                    var currentDate = startDate
                    while (currentDate <= endDate) {
                        add(currentDate)
                        currentDate = currentDate.plus(1, DateTimeUnit.DAY)
                    }
                }
            }
        }
    }

    override fun observeDayDetails(): Flow<Map<LocalDate, DayDetails>> {
        return combine(
            getAllPeriods(),
            getAllLogs()
        ) { cycles, allLogs ->
            val detailsMap = mutableMapOf<LocalDate, DayDetails>()
            val today = Clock.System.todayIn(TimeZone.currentSystemDefault())

            for (cycle in cycles) {
                // Use the cycle's end date, or 'today' if the cycle is ongoing.
                val endDate = cycle.endDate ?: today
                var currentDate = cycle.startDate

                while (currentDate <= endDate) {
                    detailsMap[currentDate] = DayDetails(isPeriodDay = true)
                    currentDate = currentDate.plus(1, DateTimeUnit.DAY)
                }
            }

            for (log in allLogs) {
                val date = log.entry.entryDate
                val existingInfo = detailsMap[date] ?: DayDetails()
                detailsMap[date] = existingInfo.copy(
                    isPeriodDay = existingInfo.isPeriodDay || log.periodLog != null,
                    hasLoggedSymptoms = log.symptomLogs.isNotEmpty(),
                    hasLoggedMedications = log.medicationLogs.isNotEmpty()
                )
            }
            detailsMap
        }
    }

    override suspend fun getAllSymptomLogs(): List<SymptomLog> {
        return symptomLogDao.getAllSymptomLogs().first().map { it.toDomain() }
    }

    override suspend fun getAllMedicationLogs(): List<MedicationLog> {
        return medicationLogDao.getAllMedicationLogs().first().map { it.toDomain() }
    }

    override suspend fun deletePeriod(id: String) {
        val periodToDelete = periodDao.getByUuid(id)
        periodToDelete?.let { period ->
            db.withTransaction {
                // Determine the end date for the query range. Ongoing periods extend up to today.
                val endDateForRange = period.endDate ?: Clock.System.todayIn(TimeZone.currentSystemDefault())

                // 1. Find all existing DailyEntry IDs that fall within the date range of the period being deleted.
                // We use .first() on the Flow to get the current snapshot.
                val entriesInPeriod = dailyEntryDao.getEntriesForDateRange(
                    period.startDate,
                    endDateForRange
                ).first()

                // 2. Delete the PeriodLogs (menstruation-specific data) for each Daily Entry in the range.
                // This keeps the core DailyEntry (mood, energy, tags) intact.
                entriesInPeriod.forEach { entry ->
                    periodLogDao.deleteLogForEntry(entry.id)
                }

                // 3. Delete the Period itself.
                periodDao.deleteByUuid(id)
            }
        }
    }

    /**
     * [DEBUG ONLY] Clears all user data and seeds the database with a fresh,
     * realistic dataset of 6 completed periods ending yesterday.
     */
    override suspend fun seedDatabaseForDebug() {
        // --- Get library items we will use (prepopulate and clear) ---
        prepopulateSymptomLibrary()
        val cramps = createOrGetSymptomInLibrary("Cramps", SymptomCategory.PAIN)
        val headache = createOrGetSymptomInLibrary("Headache", SymptomCategory.PAIN)
        val anxiety = createOrGetSymptomInLibrary("Anxiety", SymptomCategory.MOOD)
        val bloating = createOrGetSymptomInLibrary("Bloating", SymptomCategory.DIGESTIVE)
        val ibuprofen = createOrGetMedicationInLibrary("Ibuprofen")

        db.withTransaction {
            medicationLogDao.deleteAll()
            symptomLogDao.deleteAll()
            periodLogDao.deleteAll()
            dailyEntryDao.deleteAll()
            periodDao.deleteAll()
        }

        // --- Generate new data backwards from today ---
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())

        // Start date of the most recent *completed* period (e.g., 28 days ago).
        val LATEST_PERIOD_START_DAYS_AGO = 28
        var currentCycleStartDate = today.minus(LATEST_PERIOD_START_DAYS_AGO, DateTimeUnit.DAY)

        val cyclesToCreate = 6 // Generate 6 completed periods
        val allNewPeriods = mutableListOf<Period>()
        val allNewLogs = mutableListOf<FullDailyLog>()

        repeat(cyclesToCreate) { index ->
            val cycleLength = Random.nextInt(27, 33)
            val periodLength = Random.nextInt(4, 7) // Realistic period length (4-7 days)

            val cycleStartDate = currentCycleStartDate

            // All generated periods are historical and completed.
            val newPeriod = Period(
                id = uuid4().toString(),
                startDate = cycleStartDate,
                endDate = cycleStartDate.plus(periodLength - 1, DateTimeUnit.DAY),
                createdAt = Clock.System.now(),
                updatedAt = Clock.System.now()
            )
            allNewPeriods.add(newPeriod)

            // Log data for the full generated cycle length.
            val daysToLogInCycle = cycleLength

            for (dayOffset in 0 until daysToLogInCycle) {
                val currentDate = cycleStartDate.plus(dayOffset, DAY)
                val dayInCycle = dayOffset + 1
                val entryId = uuid4().toString()

                // --- Generate PeriodLog (Flow) ---
                // Flow data only exists for the duration of the periodLength.
                val isFlowDay = dayInCycle <= periodLength

                val periodLog = if (isFlowDay) {
                    PeriodLog(
                        id = uuid4().toString(),
                        entryId = entryId,
                        flowIntensity = when (dayInCycle) {
                            1 -> FlowIntensity.MEDIUM
                            2, 3 -> FlowIntensity.HEAVY
                            else -> FlowIntensity.LIGHT
                        },
                        createdAt = Clock.System.now(),
                        updatedAt = Clock.System.now()
                    )
                } else null

                // --- Generate Generic Daily Entry (Mood/Energy/etc.) ---
                var mood: Int?
                // Logic uses cycleLength to define pre-menstrual and menstrual phase windows for mood logging.
                val lowMoodWindowStart = cycleLength - 5
                val lowMoodWindowEnd = 4

                when {
                    dayInCycle >= lowMoodWindowStart || dayInCycle <= lowMoodWindowEnd -> {
                        mood = if (Random.nextFloat() < 0.90f) Random.nextInt(1, 3) else Random.nextInt(3, 5)
                    }
                    dayInCycle in 10..18 && Random.nextFloat() < 0.80f -> {
                        mood = Random.nextInt(4, 6)
                    }
                    else -> {
                        mood = Random.nextInt(3, 5)
                    }
                }
                if (Random.nextFloat() < 0.1f) mood = null
                val energy = if (mood != null && mood < 3) Random.nextInt(1, 4) else Random.nextInt(3, 6)

                val newEntry = DailyEntry(
                    id = entryId,
                    entryDate = currentDate,
                    dayInCycle = dayInCycle,
                    moodScore = mood,
                    energyLevel = if (Random.nextFloat() < 0.85f) energy else null,
                    createdAt = Clock.System.now(),
                    updatedAt = Clock.System.now()
                )

                // --- Generate Symptoms & Meds ---
                val isActualFlowDay = periodLog != null
                val symptomLogs = mutableListOf<SymptomLog>()
                if (isActualFlowDay && Random.nextFloat() < 0.8f) {
                    symptomLogs.add(SymptomLog(uuid4().toString(), entryId, cramps.id, Random.nextInt(2, 5), Clock.System.now()))
                }
                // Bloating/Anxiety/Headache are logged around their predicted phase windows
                if (dayInCycle in (cycleLength - 5)..<cycleLength && Random.nextFloat() < 0.5f) {
                    symptomLogs.add(SymptomLog(uuid4().toString(), entryId, bloating.id, Random.nextInt(2, 5), Clock.System.now()))
                }
                if ((newEntry.moodScore ?: 6) < 3 && Random.nextFloat() < 0.5f) {
                    symptomLogs.add(SymptomLog(uuid4().toString(), entryId, anxiety.id, Random.nextInt(2, 5), Clock.System.now()))
                }
                if (Random.nextFloat() < 0.15f) {
                    symptomLogs.add(SymptomLog(uuid4().toString(), entryId, headache.id, Random.nextInt(1, 4), Clock.System.now()))
                }

                val medicationLogs = mutableListOf<MedicationLog>()
                if (symptomLogs.any { it.symptomId == cramps.id || it.symptomId == headache.id } && Random.nextFloat() < 0.6f) {
                    medicationLogs.add(MedicationLog(uuid4().toString(), entryId, ibuprofen.id, Clock.System.now()))
                }

                allNewLogs.add(FullDailyLog(newEntry, periodLog, symptomLogs, medicationLogs))
            }

            // --- Update the start date for the PREVIOUS cycle ---
            currentCycleStartDate = cycleStartDate.minus(cycleLength, DateTimeUnit.DAY)
        }

        // --- Save all generated data ---
        db.withTransaction {
            // Reverse the period list so they are inserted in chronological order
            allNewPeriods.reversed().forEach { periodDao.insert(it.toEntity()) }
            allNewLogs.forEach { log ->
                dailyEntryDao.insert(log.entry.toEntity())
                log.periodLog?.let { periodLogDao.insert(it.toEntity()) }
                if (log.symptomLogs.isNotEmpty()) symptomLogDao.insertAll(log.symptomLogs.map { it.toEntity() })
                if (log.medicationLogs.isNotEmpty()) medicationLogDao.insertAll(log.medicationLogs.map { it.toEntity() })
            }
        }
    }

    override suspend fun logPeriodDay(date: LocalDate) {
        db.withTransaction {
            // Find adjacent periods or period that contains the date.
            // NOTE: We rely on getAllPeriods().first() then filtering in the repo to handle ongoing cycles correctly,
            // as Room's @Query is limited with nullable/dynamic date ranges.
            val periodBefore = getPeriodForDate(date.minus(1, DateTimeUnit.DAY))
            val periodAfter = getPeriodForDate(date.plus(1, DateTimeUnit.DAY))
            val periodContaining = getPeriodForDate(date) // Check if the date is already part of a period

            when {
                // Scenario 1: Already logged
                periodContaining != null -> {
                    // If it's the ongoing period, ensure the end date is nullified if it was closed prematurely
                    if (periodContaining.endDate != null && periodContaining.endDate!! < date) {
                        updatePeriodEndDate(periodContaining.id, null)
                    }
                    // Also ensure flow is logged for this day
                    val entry = dailyEntryDao.getEntryForDate(date).firstOrNull()?.toDomain()
                    entry?.let {
                        periodLogDao.insert(PeriodLog(
                            id = uuid4().toString(),
                            entryId = it.id,
                            flowIntensity = FlowIntensity.MEDIUM, // Default flow intensity for auto-log
                            createdAt = Clock.System.now(),
                            updatedAt = Clock.System.now()
                        ).toEntity())
                    }
                }

                // Scenario 2: Bridges two existing periods (MERGE)
                periodBefore != null && periodAfter != null -> {
                    // Update 'periodBefore' to span the new combined range (start of before, end of after)
                    updatePeriodEndDate(periodBefore.id, periodAfter.endDate)
                    // Delete the 'periodAfter' entity
                    periodDao.deleteByUuid(periodAfter.id)
                }

                // Scenario 3: Extends an existing period (either forward or backward)
                periodBefore != null -> {
                    updatePeriodEndDate(periodBefore.id, date) // Extend the end date
                }
                periodAfter != null -> {
                    updatePeriodStartDate(periodAfter.id, date) // Extend the start date
                }

                // Scenario 4: Day is an island (CREATE)
                else -> {
                    createCompletedPeriod(date, date) // Creates a 1-day period
                }
            }
        }
    }

    override suspend fun unLogPeriodDay(date: LocalDate) {
        db.withTransaction {
            val periodToModify = getPeriodForDate(date)
            periodToModify ?: return@withTransaction // Nothing to unmark

            when {
                // Scenario 1: Single-day period (DELETE)
                periodToModify.startDate == periodToModify.endDate || periodToModify.startDate == date && periodToModify.endDate == null -> {
                    // Safest to always use deletePeriod which clears associated PeriodLogs
                    deletePeriod(periodToModify.id)
                }

                // Scenario 2: Unmarking the Start Date (ADJUST START)
                periodToModify.startDate == date -> {
                    updatePeriodStartDate(periodToModify.id, date.plus(1, DateTimeUnit.DAY))
                }

                // Scenario 3: Unmarking the End Date (ADJUST END)
                periodToModify.endDate == date || periodToModify.endDate == null -> {
                    updatePeriodEndDate(periodToModify.id, date.minus(1, DateTimeUnit.DAY))
                }

                // Scenario 4: Unmarking a Middle Day (SPLIT)
                else -> {
                    // Update existing period to end at X-1
                    updatePeriodEndDate(periodToModify.id, date.minus(1, DateTimeUnit.DAY))

                    // Create a new period from X+1 to Z (end of old period)
                    periodDao.insert(Period(
                        id = uuid4().toString(),
                        startDate = date.plus(1, DateTimeUnit.DAY),
                        endDate = periodToModify.endDate,
                        createdAt = Clock.System.now(),
                        updatedAt = Clock.System.now()
                    ).toEntity())
                }
            }
            // Delete the flow log for this specific day, as it is no longer a period day
            val entryForDate = dailyEntryDao.getEntryForDate(date).firstOrNull()
            entryForDate?.let { periodLogDao.deleteLogForEntry(it.id) }
        }
    }
}
