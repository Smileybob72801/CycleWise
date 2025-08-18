package com.veleda.cyclewise.androidData.repository

import androidx.room.withTransaction
import com.benasher44.uuid.uuid4
import com.veleda.cyclewise.androidData.local.dao.CycleDao
import com.veleda.cyclewise.androidData.local.dao.DailyEntryDao
import com.veleda.cyclewise.androidData.local.dao.MedicationDao
import com.veleda.cyclewise.androidData.local.dao.SymptomDao
import com.veleda.cyclewise.androidData.local.database.CycleDatabase
import com.veleda.cyclewise.androidData.local.entities.toDomain
import com.veleda.cyclewise.androidData.local.entities.toEntity
import com.veleda.cyclewise.domain.models.Cycle
import com.veleda.cyclewise.domain.models.FullDailyLog
import com.veleda.cyclewise.domain.repository.CycleRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.datetime.*
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class RoomCycleRepository(
    private val db: CycleDatabase,
    private val cycleDao: CycleDao,
    private val dailyEntryDao: DailyEntryDao,
    private val symptomDao: SymptomDao,
    private val medicationDao: MedicationDao
) : CycleRepository {
    // This class now correctly implements the updated interface
    // All Cycle methods are unchanged and correct.
    override suspend fun getAllCycles(): List<Cycle> =
        cycleDao.getAllCycles().first().map { it.toDomain() }

    override suspend fun getCycleById(cycleId: String): Cycle {
        val entity = cycleDao.getByUuid(cycleId)
            ?: throw NoSuchElementException("Cycle $cycleId not found")
        return entity.toDomain()
    }

    @OptIn(ExperimentalTime::class)
    override suspend fun startNewCycle(startDate: LocalDate): Cycle {
        val now = Clock.System.now()
        val uuid = uuid4().toString() // Uses the imported KMP library
        val domainCycle = Cycle(
            id = uuid,
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

    // --- NEW AND CORRECTED METHODS ---

    override suspend fun getFullLogForDate(date: LocalDate): FullDailyLog? {
        val entryEntity = dailyEntryDao.getEntryForDate(date).firstOrNull() ?: return null
        val symptoms = symptomDao.getSymptomsForEntry(entryEntity.id).firstOrNull() ?: emptyList()
        val medications = medicationDao.getMedicationsForEntry(entryEntity.id).firstOrNull() ?: emptyList()
        return FullDailyLog(
            entry = entryEntity.toDomain(),
            symptoms = symptoms.map { it.toDomain() },
            medications = medications.map { it.toDomain() }
        )
    }

    override suspend fun saveFullLog(log: FullDailyLog) {
        db.withTransaction {
            dailyEntryDao.insert(log.entry.toEntity())
            symptomDao.deleteSymptomsForEntry(log.entry.id)
            if (log.symptoms.isNotEmpty()) {
                symptomDao.insertSymptoms(log.symptoms.map { it.toEntity() })
            }
            medicationDao.deleteMedicationsForEntry(log.entry.id)
            if (log.medications.isNotEmpty()) {
                medicationDao.insertMedications(log.medications.map { it.toEntity() })
            }
        }
    }

    override suspend fun getLogsForMonth(yearMonth: YearMonth): List<FullDailyLog> {
        val startDate = yearMonth.firstDay
        val endDate = yearMonth.lastDay

        val entryEntities = dailyEntryDao.getEntriesForDateRange(startDate, endDate).firstOrNull() ?: return emptyList()
        if (entryEntities.isEmpty()) return emptyList()

        val entryIds = entryEntities.map { it.id }
        val allSymptoms = symptomDao.getSymptomsForEntries(entryIds).firstOrNull()?.groupBy { it.entryId } ?: emptyMap()
        val allMedications = medicationDao.getMedicationsForEntries(entryIds).firstOrNull()?.groupBy { it.entryId } ?: emptyMap()

        return entryEntities.map { entryEntity ->
            FullDailyLog(
                entry = entryEntity.toDomain(),
                symptoms = allSymptoms[entryEntity.id]?.map { it.toDomain() } ?: emptyList(),
                medications = allMedications[entryEntity.id]?.map { it.toDomain() } ?: emptyList()
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
}