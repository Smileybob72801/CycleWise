package com.veleda.cyclewise.androidData.repository

import com.veleda.cyclewise.androidData.local.dao.CycleDao
import com.veleda.cyclewise.androidData.local.dao.DailyEntryDao
import com.veleda.cyclewise.androidData.local.entities.CycleEntity
import com.veleda.cyclewise.androidData.local.entities.toDomain
import com.veleda.cyclewise.androidData.local.entities.toEntity
import com.veleda.cyclewise.domain.models.Cycle
import com.veleda.cyclewise.domain.models.DailyEntry
import com.veleda.cyclewise.domain.repository.CycleRepository
import kotlinx.coroutines.flow.first
import kotlinx.datetime.LocalDate
import java.util.UUID
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class RoomCycleRepository(
    private val cycleDao: CycleDao,
    private val dailyEntryDao: DailyEntryDao,
) : CycleRepository {
    override suspend fun getAllCycles(): List<Cycle> =
        cycleDao
            .getAllCycles()
            .first()
            .map { it.toDomain()}

    override suspend fun getCycleById(cycleId: String): Cycle {
        val entity = cycleDao.getByUuid(cycleId)
            ?: throw NoSuchElementException("Cycle $cycleId not found")
        return entity.toDomain()
    }

    @OptIn(ExperimentalTime::class)
    override suspend fun startNewCycle(startDate: LocalDate): Cycle {
        val now = Clock.System.now()
        val uuid = UUID.randomUUID().toString()

        val domainCycle = Cycle(
            id = uuid,
            startDate = startDate,
            endDate = null,
            createdAt = now,
            updatedAt = now
        )

        val entityCycle = domainCycle.toEntity()

        cycleDao.insert(entityCycle)

        return domainCycle
    }

    @OptIn(ExperimentalTime::class)
    override suspend fun endCycle(cycleId: String, endDate: LocalDate): Cycle? {
        val existing = cycleDao.getByUuid(cycleId) ?: return null
        val updated = existing.copy(endDate = endDate)
        cycleDao.update(updated)
        return updated.toDomain()
    }

    override suspend fun getCurrentlyOngoingCycle(): Cycle? {
        // returns Flow<CycleEntity?>; take first emission
        val entity = cycleDao.getOngoingCycle().first()
        return entity?.toDomain()
    }

    override suspend fun getEntryForDate(date: LocalDate): DailyEntry? {
        // .first() will throw if the flow is empty, so we use .firstOrNull()
        return dailyEntryDao.getEntryForDate(date).first()?.toDomain()
    }

    override suspend fun getEntriesForCycle(cycleId: String): List<DailyEntry> {
        return dailyEntryDao.getEntriesForCycle(cycleId).first().map { it.toDomain() }
    }

    override suspend fun saveEntry(entry: DailyEntry) {
        dailyEntryDao.insert(entry.toEntity())
    }
}