package com.veleda.cyclewise.domain.repository

import com.veleda.cyclewise.domain.models.Cycle
import kotlinx.datetime.LocalDate

/**
 * Shared interface for accessing and modifying cycle data.
 * Platform-specific implementations will handle persistence.
 */
interface CycleRepository {

    /** Returns all known cycles, sorted by start date descending. */
    suspend fun getAllCycles(): List<Cycle>

    /** Returns a single cycle given its primary Id. */
    suspend fun getCycleById(cycleId: String): Cycle

    /** Starts a new cycle on the given start date. */
    suspend fun startNewCycle(startDate: LocalDate) : Cycle

    /** Marks an existing cycle as ended on the given date. */
    suspend fun endCycle(cycleId: String, endDate: LocalDate): Cycle?

    /** Returns the currently active (non-ended) cycle, if any. */
    suspend fun getCurrentlyOngoingCycle(): Cycle?
}