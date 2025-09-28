package com.veleda.cyclewise.androidData.repository

import androidx.room.withTransaction
import com.benasher44.uuid.uuid4
import com.veleda.cyclewise.androidData.local.dao.CycleDao
import com.veleda.cyclewise.androidData.local.dao.DailyEntryDao
import com.veleda.cyclewise.androidData.local.dao.MedicationDao
import com.veleda.cyclewise.androidData.local.dao.MedicationLogDao
import com.veleda.cyclewise.androidData.local.dao.SymptomDao
import com.veleda.cyclewise.androidData.local.dao.SymptomLogDao
import com.veleda.cyclewise.androidData.local.database.CycleDatabase
import com.veleda.cyclewise.androidData.local.entities.toDomain
import com.veleda.cyclewise.androidData.local.entities.toEntity
import com.veleda.cyclewise.domain.models.Cycle
import com.veleda.cyclewise.domain.models.DailyEntry
import com.veleda.cyclewise.domain.models.FlowIntensity
import com.veleda.cyclewise.domain.models.MedicationLog
import com.veleda.cyclewise.domain.models.SymptomLog
import com.veleda.cyclewise.domain.models.FullDailyLog
import com.veleda.cyclewise.domain.models.Medication
import com.veleda.cyclewise.domain.models.Symptom
import com.veleda.cyclewise.domain.models.SymptomCategory
import com.veleda.cyclewise.domain.repository.CycleRepository
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

class RoomCycleRepository(
    private val db: CycleDatabase,
    private val cycleDao: CycleDao,
    private val dailyEntryDao: DailyEntryDao,
    private val symptomDao: SymptomDao,
    private val medicationDao: MedicationDao,
    private val medicationLogDao: MedicationLogDao,
    private val symptomLogDao: SymptomLogDao,
) : CycleRepository {
    override fun getAllCycles(): Flow<List<Cycle>> {
        return cycleDao.getAllCycles().map { entityList ->
            entityList.map { it.toDomain() }
        }
    }

    override suspend fun getCycleById(cycleId: String): Cycle {
        val entity = cycleDao.getByUuid(cycleId)
            ?: throw NoSuchElementException("Cycle $cycleId not found")
        return entity.toDomain()
    }

    @OptIn(ExperimentalTime::class)
    override suspend fun startNewCycle(startDate: LocalDate): Cycle {
        val now = Clock.System.now()
        val cycleUuid  = uuid4().toString()

        val domainCycle = Cycle(
            id = cycleUuid ,
            startDate = startDate,
            endDate = null,
            createdAt = now,
            updatedAt = now
        )

        cycleDao.insert(domainCycle.toEntity())

        return domainCycle
    }

    override suspend fun endCycle(cycleId: String, endDate: LocalDate): Cycle? {
        val existing = cycleDao.getByUuid(cycleId) ?: return null
        val updated = existing.copy(endDate = endDate)
        cycleDao.update(updated)
        return updated.toDomain()
    }

    override suspend fun getCurrentlyOngoingCycle(): Cycle? {
        return cycleDao.getOngoingCycle().firstOrNull()?.toDomain()
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
    override suspend fun createCompletedCycle(startDate: LocalDate, endDate: LocalDate): Cycle {
        val now = Clock.System.now()
        val domainCycle = Cycle(
            id = uuid4().toString(),
            startDate = startDate,
            endDate = endDate, // The end date is provided directly
            createdAt = now,
            updatedAt = now
        )
        cycleDao.insert(domainCycle.toEntity())
        return domainCycle
    }

    override suspend fun isDateRangeAvailable(startDate: LocalDate, endDate: LocalDate): Boolean {
        val count = cycleDao.getOverlappingCyclesCount(startDate, endDate)
        return count == 0
    }

    override suspend fun updateCycleEndDate(cycleId: String, endDate: LocalDate?): Cycle? {
        val existing = cycleDao.getByUuid(cycleId) ?: return null
        // The .copy() method can handle a nullable value perfectly.
        val updated = existing.copy(endDate = endDate, updatedAt = Clock.System.now())
        cycleDao.update(updated)
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
        // This will automatically re-run whenever the 'cycles' table changes.
        return cycleDao.getAllCycles().map { cycles ->
            // Use buildSet for an efficient way to create the set of dates.
            buildSet {
                val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
                for (cycle in cycles) {
                    val startDate = cycle.startDate
                    // CRITICAL FIX: Use 'today' as the end date for ongoing cycles.
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
            getAllCycles(),
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
}
