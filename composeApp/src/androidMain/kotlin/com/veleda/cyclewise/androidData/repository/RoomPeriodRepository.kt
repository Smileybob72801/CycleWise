package com.veleda.cyclewise.androidData.repository

import androidx.room.withTransaction
import com.benasher44.uuid.uuid4
import com.veleda.cyclewise.androidData.local.dao.PeriodDao
import com.veleda.cyclewise.androidData.local.dao.DailyEntryDao
import com.veleda.cyclewise.androidData.local.dao.MedicationDao
import com.veleda.cyclewise.androidData.local.dao.MedicationLogDao
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
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DateTimeUnit.Companion.DAY
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import kotlin.random.Random

class RoomPeriodRepository(
    private val db: PeriodDatabase,
    private val periodDao: PeriodDao,
    private val dailyEntryDao: DailyEntryDao,
    private val symptomDao: SymptomDao,
    private val medicationDao: MedicationDao,
    private val medicationLogDao: MedicationLogDao,
    private val symptomLogDao: SymptomLogDao,
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
        val symptomLogs = symptomLogDao.getLogsForEntry(entryEntity.id).firstOrNull() ?: emptyList()
        val medicationLogs = medicationLogDao.getLogsForEntry(entryEntity.id).firstOrNull() ?: emptyList()
        return FullDailyLog(
            entry = entryEntity.toDomain(),
            symptomLogs = symptomLogs.map { it.toDomain() },
            medicationLogs = medicationLogs.map { it.toDomain() }
        )
    }

    override suspend fun saveFullLog(log: FullDailyLog) {
        db.withTransaction {
            dailyEntryDao.insert(log.entry.toEntity())
            symptomLogDao.deleteLogsForEntry(log.entry.id)
            if (log.symptomLogs.isNotEmpty()) {
                symptomLogDao.insertAll(log.symptomLogs.map { it.toEntity() })
            }
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

    override suspend fun updatePeriodEndDate(periodId: String, endDate: LocalDate?): Period? {
        val existing = periodDao.getByUuid(periodId) ?: return null
        // The .copy() method can handle a nullable value perfectly.
        val updated = existing.copy(endDate = endDate, updatedAt = Clock.System.now())
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
                // Combine the flows for symptoms and medications related to these entries
                combine(
                    symptomLogDao.getLogsForEntries(entryIds),
                    medicationLogDao.getLogsForEntries(entryIds)
                ) { symptoms, medications ->
                    val symptomsById = symptoms.groupBy { it.entryId }
                    val medicationsById = medications.groupBy { it.entryId }

                    entries.map { entry ->
                        FullDailyLog(
                            entry = entry.toDomain(),
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
                    // CRITICAL FIX: Use 'today' as the end date for ongoing periods.
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
                    isPeriodDay = existingInfo.isPeriodDay || log.entry.flowIntensity != null,
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

    /**
     * [DEBUG ONLY] Clears all user data and seeds the database with a fresh,
     * realistic dataset of 6 completed periods ending yesterday.
     */
    override suspend fun seedDatabaseForDebug() {
        // --- 1. Get library items we will use ---
        prepopulateSymptomLibrary()
        val cramps = createOrGetSymptomInLibrary("Cramps", SymptomCategory.PAIN)
        val headache = createOrGetSymptomInLibrary("Headache", SymptomCategory.PAIN)
        val anxiety = createOrGetSymptomInLibrary("Anxiety", SymptomCategory.MOOD)
        val bloating = createOrGetSymptomInLibrary("Bloating", SymptomCategory.DIGESTIVE)
        val ibuprofen = createOrGetMedicationInLibrary("Ibuprofen")

        // --- 2. Clear all existing user data in a single transaction ---
        db.withTransaction {
            medicationLogDao.deleteAll()
            symptomLogDao.deleteAll()
            dailyEntryDao.deleteAll()
            periodDao.deleteAll()
        }

        // --- 3. Generate new data backwards from today ---
        // This ensures the data is always fresh and relevant.
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        var nextCycleStartDate = today.minus(Random.nextInt(10, 20), DateTimeUnit.DAY) // Start the *current* cycle 10-20 days ago

        val cyclesToCreate = 6
        val allNewPeriods = mutableListOf<Period>()
        val allNewLogs = mutableListOf<FullDailyLog>()

        // This loop works backwards in time.
        repeat(cyclesToCreate) { index ->
            val isOngoingCycle = index == 0 // The first cycle we generate is the current, ongoing one.

            val cycleLength = Random.nextInt(27, 33)
            val periodLength = Random.nextInt(4, 7)
            val cycleStartDate = nextCycleStartDate

            val newPeriod: Period
            if (isOngoingCycle) {
                // This is the current, active cycle. It has no end date.
                newPeriod = Period(
                    id = uuid4().toString(),
                    startDate = cycleStartDate,
                    endDate = null, // Ongoing cycle
                    createdAt = Clock.System.now(),
                    updatedAt = Clock.System.now()
                )
            } else {
                // These are the historical, completed periods.
                val periodEndDate = cycleStartDate.plus(periodLength - 1, DateTimeUnit.DAY)
                newPeriod = Period(
                    id = uuid4().toString(),
                    startDate = cycleStartDate,
                    endDate = periodEndDate,
                    createdAt = Clock.System.now(),
                    updatedAt = Clock.System.now()
                )
            }
            allNewPeriods.add(newPeriod)

            // For an ongoing cycle, only log up to today. For past periods, log the full length.
            val daysToLogInCycle = if (isOngoingCycle) cycleStartDate.daysUntil(today) + 1 else cycleLength

            for (dayOffset in 0 until daysToLogInCycle) {
                val currentDate = cycleStartDate.plus(dayOffset, DAY)
                val dayInCycle = dayOffset + 1

                // --- Enhanced Realistic Pattern Generation ---
                var mood: Int?
                // Define the premenstrual (luteal) and early menstrual phases where mood is often lower.
                val lowMoodWindowStart = cycleLength - 5 // 5 days before next cycle
                val lowMoodWindowEnd = 4 // First 4 days of cycle

                when {
                    // Create a strong, contiguous block of low mood data across the cycle boundary.
                    dayInCycle >= lowMoodWindowStart || dayInCycle <= lowMoodWindowEnd -> {
                        // Use a very high probability to ensure the pattern is strong enough to be detected.
                        mood = if (Random.nextFloat() < 0.90f) Random.nextInt(1, 3) else Random.nextInt(3, 5)
                    }
                    // A high mood window around ovulation.
                    dayInCycle in 10..18 && Random.nextFloat() < 0.80f -> {
                        mood = Random.nextInt(4, 6)
                    }
                    // Default mood for other days.
                    else -> {
                        mood = Random.nextInt(3, 5)
                    }
                }

                // Add some randomness by occasionally having no logged mood.
                if (Random.nextFloat() < 0.1f) {
                    mood = null
                }

                // Energy is often correlated with mood.
                val energy = if (mood != null && mood < 3) Random.nextInt(1, 4) else Random.nextInt(3, 6)

                val newEntry = DailyEntry(
                    id = uuid4().toString(),
                    entryDate = currentDate,
                    dayInCycle = dayInCycle,
                    flowIntensity = if (dayInCycle <= periodLength && newPeriod.endDate != null) FlowIntensity.entries.random() else null,
                    moodScore = mood,
                    energyLevel = if (Random.nextFloat() < 0.85f) energy else null, // Not always logged
                    spotting = dayInCycle > periodLength && Random.nextFloat() < 0.05f,
                    createdAt = Clock.System.now(),
                    updatedAt = Clock.System.now()
                )

                val symptomLogs = mutableListOf<SymptomLog>()
                // Cramps are common during the period.
                if (dayInCycle <= periodLength && newPeriod.endDate != null && Random.nextFloat() < 0.8f) symptomLogs.add(SymptomLog(uuid4().toString(), newEntry.id, cramps.id, Random.nextInt(2, 5), Clock.System.now()))
                // Bloating is common before the period.
                if (dayInCycle in (cycleLength - 5)..<cycleLength && Random.nextFloat() < 0.5f) symptomLogs.add(SymptomLog(uuid4().toString(), newEntry.id, bloating.id, Random.nextInt(2, 5), Clock.System.now()))
                // Link anxiety to low mood scores for a more realistic correlation.
                if ((newEntry.moodScore ?: 6) < 3 && Random.nextFloat() < 0.5f) symptomLogs.add(SymptomLog(uuid4().toString(), newEntry.id, anxiety.id, Random.nextInt(2, 5), Clock.System.now()))
                // Random headaches.
                if (Random.nextFloat() < 0.15f) symptomLogs.add(SymptomLog(uuid4().toString(), newEntry.id, headache.id, Random.nextInt(1, 4), Clock.System.now()))

                val medicationLogs = mutableListOf<MedicationLog>()
                // Taking medication is often linked to pain symptoms.
                if (symptomLogs.any { it.symptomId == cramps.id || it.symptomId == headache.id } && Random.nextFloat() < 0.6f) {
                    medicationLogs.add(MedicationLog(uuid4().toString(), newEntry.id, ibuprofen.id, Clock.System.now()))
                }

                allNewLogs.add(FullDailyLog(newEntry, symptomLogs, medicationLogs))
            }

            // Set up the start date for the *previous* cycle for the next loop iteration.
            nextCycleStartDate = cycleStartDate.minus(cycleLength, DateTimeUnit.DAY)
        }

        // --- 4. Save all generated data in a single transaction ---
        db.withTransaction {
            // Reverse the cycle list so they are inserted in chronological order
            allNewPeriods.reversed().forEach { periodDao.insert(it.toEntity()) }
            allNewLogs.forEach { log ->
                dailyEntryDao.insert(log.entry.toEntity())
                if (log.symptomLogs.isNotEmpty()) symptomLogDao.insertAll(log.symptomLogs.map { it.toEntity() })
                if (log.medicationLogs.isNotEmpty()) medicationLogDao.insertAll(log.medicationLogs.map { it.toEntity() })
            }
        }
    }
}
