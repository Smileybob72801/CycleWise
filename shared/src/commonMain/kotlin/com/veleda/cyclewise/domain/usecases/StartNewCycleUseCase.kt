package com.veleda.cyclewise.domain.usecases

import com.benasher44.uuid.uuid4
import com.veleda.cyclewise.domain.models.Cycle
import com.veleda.cyclewise.domain.repository.CycleRepository
import kotlinx.datetime.LocalDate
import kotlin.random.Random
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Starts a new menstrual cycle at the given [startDate].
 * Fails if there's already an ongoing cycle.
 */
class StartNewCycleUseCase (private val repository: CycleRepository)
{
    /**
     * Attempts to start a new cycle.
     * @return the new cycle, or null if a cycle is already ongoing
     */
    @OptIn(ExperimentalTime::class)
    suspend operator fun invoke(startDate: LocalDate): Cycle? {
        val currentlyOngoingCycle = repository.getCurrentlyOngoingCycle()

        if (currentlyOngoingCycle != null) {
            return null
        }
        val now = Clock.System.now()

        val newCycle = Cycle(
            id = uuid4().toString(),
            startDate = startDate,
            endDate = null,
            createdAt = now,
            updatedAt = now
        )

        return repository.startNewCycle(startDate = newCycle.startDate)
    }
}