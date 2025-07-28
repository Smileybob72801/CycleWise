package com.veleda.cyclewise.androidData.repository

import com.veleda.cyclewise.domain.models.Cycle
import com.veleda.cyclewise.domain.repository.CycleRepository
import kotlinx.datetime.LocalDate
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Simple in-memory implementation of CycleRepository for Android testing.
 * This version does not persist data and resets on app restart.
 */
class InMemoryCycleRepository : CycleRepository {

    private val cycleStorage = ConcurrentHashMap<String, Cycle>()

    override suspend fun getAllCycles(): List<Cycle> {
        return cycleStorage.values.sortedByDescending { it.startDate }
    }

    override suspend fun getCycleById(cycleId: String): Cycle {
        return cycleStorage[cycleId]
            ?: throw IllegalArgumentException("Cycle with ID $cycleId not found")
    }

    override suspend fun startNewCycle(startDate: LocalDate): Cycle {
        val ongoing = getCurrentlyOngoingCycle()
        require(ongoing == null) { "Cannot start a new cycle while another is ongoing." }

        val newCycle = Cycle(
            id = UUID.randomUUID().toString(),
            startDate = startDate,
            endDate = null
        )
        cycleStorage[newCycle.id] = newCycle
        return newCycle
    }

    override suspend fun endCycle(cycleId: String, endDate: LocalDate): Cycle? {
        val existing = cycleStorage[cycleId] ?: return null
        val updated = existing.copy(endDate = endDate)
        cycleStorage[cycleId] = updated
        return updated
    }

    override suspend fun getCurrentlyOngoingCycle(): Cycle? {
        return cycleStorage.values.firstOrNull { it.endDate == null }
    }
}