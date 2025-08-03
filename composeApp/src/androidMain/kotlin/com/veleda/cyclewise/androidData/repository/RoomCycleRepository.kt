package com.veleda.cyclewise.androidData.repository

import com.veleda.cyclewise.androidData.local.dao.CycleDao
import com.veleda.cyclewise.androidData.local.entities.CycleEntity
import com.veleda.cyclewise.androidData.local.entities.toDomain
import com.veleda.cyclewise.androidData.local.entities.toEntity
import com.veleda.cyclewise.domain.models.Cycle
import com.veleda.cyclewise.domain.repository.CycleRepository
import kotlinx.coroutines.flow.first
import kotlinx.datetime.LocalDate
import java.util.UUID
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class RoomCycleRepository(
    private val dao: CycleDao
) : CycleRepository {
    override suspend fun getAllCycles(): List<Cycle> =
        dao
            .getAllCycles()
            .first()
            .map { it.toDomain()}

    override suspend fun getCycleById(cycleId: String): Cycle {
        val entity = dao.getByUuid(cycleId)
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

        dao.insert(entityCycle)

        return domainCycle
    }

    @OptIn(ExperimentalTime::class)
    override suspend fun endCycle(cycleId: String, endDate: LocalDate): Cycle? {
        val existing = dao.getByUuid(cycleId) ?: return null
        val updated = existing.copy(endDate = endDate)
        dao.update(updated)
        return updated.toDomain()
    }

    override suspend fun getCurrentlyOngoingCycle(): Cycle? {
        // returns Flow<CycleEntity?>; take first emission
        val entity = dao.getOngoingCycle().first()
        return entity?.toDomain()
    }
}