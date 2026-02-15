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
import com.veleda.cyclewise.androidData.local.dao.WaterIntakeDao
import com.veleda.cyclewise.androidData.local.database.PeriodDatabase
import com.veleda.cyclewise.androidData.local.entities.toDomain
import com.veleda.cyclewise.androidData.local.entities.toEntity
import com.veleda.cyclewise.domain.models.Period
import com.veleda.cyclewise.domain.models.DailyEntry
import com.veleda.cyclewise.domain.models.FlowIntensity
import com.veleda.cyclewise.domain.models.MedicationLog
import com.veleda.cyclewise.domain.models.SymptomLog
import com.veleda.cyclewise.domain.models.WaterIntake
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
import com.veleda.cyclewise.domain.CyclePhaseCalculator
import com.veleda.cyclewise.domain.models.CyclePhase
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

/**
 * Room-backed implementation of [PeriodRepository].
 *
 * All data access is routed through SQLCipher-encrypted DAOs. This class lives inside
 * the session scope — it is created after passphrase unlock and destroyed on logout/autolock.
 *
 * Key behaviors:
 * - [saveFullLog] uses delete-then-insert within a Room transaction for child records.
 * - [logPeriodDay] implements a 4-scenario state machine (see [PeriodRepository] KDoc).
 * - [unLogPeriodDay] implements a 4-scenario deletion/split state machine.
 * - [observeDayDetails] combines period ranges and log data into the calendar's source of truth.
 * - [seedDatabaseForDebug] is destructive and generates 6 months of test data.
 */
class RoomPeriodRepository(
    private val db: PeriodDatabase,
    private val periodDao: PeriodDao,
    private val dailyEntryDao: DailyEntryDao,
    private val symptomDao: SymptomDao,
    private val medicationDao: MedicationDao,
    private val medicationLogDao: MedicationLogDao,
    private val symptomLogDao: SymptomLogDao,
    private val periodLogDao: PeriodLogDao,
    private val waterIntakeDao: WaterIntakeDao,
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
            dailyEntryDao.insert(log.entry.toEntity())

            periodLogDao.deleteLogForEntry(log.entry.id)
            log.periodLog?.let { periodLog ->
                periodLogDao.insert(periodLog.toEntity())
            }

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
            endDate = endDate,
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

    /** Finds the period that encompasses the given [date], accounting for ongoing periods. */
    private suspend fun getPeriodForDate(date: LocalDate): Period? {
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        return periodDao.getAllPeriods().firstOrNull()?.find { date in (it.startDate..(it.endDate ?: today)) }?.toDomain()
    }

    /** Updates the start date of the period identified by [periodId]. */
    private suspend fun updatePeriodStartDate(periodId: String, newStartDate: LocalDate): Period? {
        val existing = periodDao.getByUuid(periodId) ?: return null
        val updated = existing.copy(startDate = newStartDate, updatedAt = Clock.System.now())
        periodDao.update(updated)
        return updated.toDomain()
    }

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
        )

        defaultSymptoms.forEach { symptom ->
            symptomDao.insert(symptom.toEntity())
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getAllLogs(): Flow<List<FullDailyLog>> {
        return dailyEntryDao.getAllEntries().flatMapLatest { entries ->
            if (entries.isEmpty()) {
                flowOf(emptyList())
            } else {
                val entryIds = entries.map { it.id }
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
        return periodDao.getAllPeriods().map { cycles ->
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
            val avgCycleLength = CyclePhaseCalculator.averageCycleLength(cycles)

            for (cycle in cycles) {
                val endDate = cycle.endDate ?: today
                var currentDate = cycle.startDate

                while (currentDate <= endDate) {
                    detailsMap[currentDate] = DayDetails(
                        isPeriodDay = true,
                        cyclePhase = CyclePhase.MENSTRUATION
                    )
                    currentDate = currentDate.plus(1, DateTimeUnit.DAY)
                }
            }

            for (log in allLogs) {
                val date = log.entry.entryDate
                val existingInfo = detailsMap[date] ?: DayDetails()
                val phase = existingInfo.cyclePhase
                    ?: CyclePhaseCalculator.calculatePhase(date, cycles, avgCycleLength)
                detailsMap[date] = existingInfo.copy(
                    isPeriodDay = existingInfo.isPeriodDay || log.periodLog != null,
                    hasLoggedSymptoms = log.symptomLogs.isNotEmpty(),
                    hasLoggedMedications = log.medicationLogs.isNotEmpty(),
                    cyclePhase = phase
                )
            }

            // Pass 3: fill phase for unlogged, non-period days in tracked cycles
            if (cycles.isNotEmpty()) {
                val earliest = cycles.minOf { it.startDate }
                var fillDate = earliest
                while (fillDate <= today) {
                    if (fillDate !in detailsMap) {
                        val phase = CyclePhaseCalculator.calculatePhase(fillDate, cycles, avgCycleLength)
                        if (phase != null) {
                            detailsMap[fillDate] = DayDetails(cyclePhase = phase)
                        }
                    }
                    fillDate = fillDate.plus(1, DateTimeUnit.DAY)
                }
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
                val endDateForRange = period.endDate ?: Clock.System.todayIn(TimeZone.currentSystemDefault())

                val entriesInPeriod = dailyEntryDao.getEntriesForDateRange(
                    period.startDate,
                    endDateForRange
                ).first()

                entriesInPeriod.forEach { entry ->
                    periodLogDao.deleteLogForEntry(entry.id)
                }

                periodDao.deleteByUuid(id)
            }
        }
    }

    /**
     * **[DEBUG ONLY]** Clears all user data and seeds the database with 6 completed
     * periods spanning several months, each with daily entries, symptoms, and medications.
     */
    override suspend fun seedDatabaseForDebug() {
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

        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())

        val LATEST_PERIOD_START_DAYS_AGO = 28
        var currentCycleStartDate = today.minus(LATEST_PERIOD_START_DAYS_AGO, DateTimeUnit.DAY)

        val cyclesToCreate = 6
        val allNewPeriods = mutableListOf<Period>()
        val allNewLogs = mutableListOf<FullDailyLog>()

        repeat(cyclesToCreate) { index ->
            val cycleLength = Random.nextInt(27, 33)
            val periodLength = Random.nextInt(4, 7)

            val cycleStartDate = currentCycleStartDate

            val newPeriod = Period(
                id = uuid4().toString(),
                startDate = cycleStartDate,
                endDate = cycleStartDate.plus(periodLength - 1, DateTimeUnit.DAY),
                createdAt = Clock.System.now(),
                updatedAt = Clock.System.now()
            )
            allNewPeriods.add(newPeriod)

            val daysToLogInCycle = cycleLength

            for (dayOffset in 0 until daysToLogInCycle) {
                val currentDate = cycleStartDate.plus(dayOffset, DAY)
                val dayInCycle = dayOffset + 1
                val entryId = uuid4().toString()

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

                var mood: Int?
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

                val isActualFlowDay = periodLog != null
                val symptomLogs = mutableListOf<SymptomLog>()
                if (isActualFlowDay && Random.nextFloat() < 0.8f) {
                    symptomLogs.add(SymptomLog(uuid4().toString(), entryId, cramps.id, Random.nextInt(2, 5), Clock.System.now()))
                }
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

            currentCycleStartDate = cycleStartDate.minus(cycleLength, DateTimeUnit.DAY)
        }

        db.withTransaction {
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
            val periodBefore = getPeriodForDate(date.minus(1, DateTimeUnit.DAY))
            val periodAfter = getPeriodForDate(date.plus(1, DateTimeUnit.DAY))
            val periodContaining = getPeriodForDate(date)

            when {
                // Scenario 1: Already logged
                periodContaining != null -> {
                    if (periodContaining.endDate != null && periodContaining.endDate!! < date) {
                        updatePeriodEndDate(periodContaining.id, null)
                    }
                    val entry = dailyEntryDao.getEntryForDate(date).firstOrNull()?.toDomain()
                    entry?.let {
                        periodLogDao.insert(PeriodLog(
                            id = uuid4().toString(),
                            entryId = it.id,
                            flowIntensity = FlowIntensity.MEDIUM,
                            createdAt = Clock.System.now(),
                            updatedAt = Clock.System.now()
                        ).toEntity())
                    }
                }

                // Scenario 2: Bridges two existing periods (MERGE)
                periodBefore != null && periodAfter != null -> {
                    updatePeriodEndDate(periodBefore.id, periodAfter.endDate)
                    periodDao.deleteByUuid(periodAfter.id)
                }

                // Scenario 3: Extends an existing period (either forward or backward)
                periodBefore != null -> {
                    updatePeriodEndDate(periodBefore.id, date)
                }
                periodAfter != null -> {
                    updatePeriodStartDate(periodAfter.id, date)
                }

                // Scenario 4: Day is an island (CREATE)
                else -> {
                    createCompletedPeriod(date, date)
                }
            }
        }
    }

    override suspend fun unLogPeriodDay(date: LocalDate) {
        db.withTransaction {
            val periodToModify = getPeriodForDate(date)
            periodToModify ?: return@withTransaction

            when {
                // Scenario 1: Single-day period (DELETE)
                periodToModify.startDate == periodToModify.endDate || periodToModify.startDate == date && periodToModify.endDate == null -> {
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
                    updatePeriodEndDate(periodToModify.id, date.minus(1, DateTimeUnit.DAY))

                    periodDao.insert(Period(
                        id = uuid4().toString(),
                        startDate = date.plus(1, DateTimeUnit.DAY),
                        endDate = periodToModify.endDate,
                        createdAt = Clock.System.now(),
                        updatedAt = Clock.System.now()
                    ).toEntity())
                }
            }
            val entryForDate = dailyEntryDao.getEntryForDate(date).firstOrNull()
            entryForDate?.let { periodLogDao.deleteLogForEntry(it.id) }
        }
    }

    override suspend fun upsertWaterIntake(intake: WaterIntake) {
        waterIntakeDao.upsert(intake.toEntity())
    }

    override suspend fun getWaterIntakeForDates(dates: List<LocalDate>): List<WaterIntake> {
        return waterIntakeDao.getForDates(dates.map { it.toString() }).map { it.toDomain() }
    }
}
