package com.veleda.cyclewise.domain.usecases

import com.veleda.cyclewise.domain.models.Cycle
import com.veleda.cyclewise.domain.repository.CycleRepository
import kotlinx.datetime.LocalDate
import kotlin.random.Random

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
    suspend operator fun invoke(startDate: LocalDate): Cycle? {
        val currentlyOngoingCycle = repository.getCurrentlyOngoingCycle()

        if (currentlyOngoingCycle != null) {
            return null
        }

        val newCycle = Cycle(
            id = generateFakeId(),
            startDate = startDate,
            endDate = null
        )

        return repository.startNewCycle(startDate = newCycle.startDate)
    }

    private fun generateFakeId(): String {
        // Temporary ID generation — replace with UUID service or platform impl later
        return Random.nextLong().toString()
    }
}