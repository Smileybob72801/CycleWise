package com.veleda.cyclewise.domain.usecases

import com.benasher44.uuid.uuid4
import com.veleda.cyclewise.domain.models.Period
import com.veleda.cyclewise.domain.repository.PeriodRepository
import kotlinx.datetime.LocalDate
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Starts a new menstrual period beginning on [startDate].
 *
 * **Precondition:** No ongoing period exists (endDate == null).
 * **Postcondition:** A new [Period] with null endDate is persisted.
 *
 * This operation is **not idempotent** — calling it twice while no ongoing period
 * exists would create two distinct periods (though the second call would fail
 * because the first would still be ongoing).
 */
class StartNewPeriodUseCase (private val repository: PeriodRepository)
{
    /**
     * @return the newly created [Period], or null if an ongoing period already exists.
     */
    @OptIn(ExperimentalTime::class)
    suspend operator fun invoke(startDate: LocalDate): Period? {
        val currentlyOngoingPeriod = repository.getCurrentlyOngoingPeriod()

        if (currentlyOngoingPeriod != null) {
            return null
        }
        val now = Clock.System.now()

        val newPeriod = Period(
            id = uuid4().toString(),
            startDate = startDate,
            endDate = null,
            createdAt = now,
            updatedAt = now
        )

        return repository.startNewPeriod(startDate = newPeriod.startDate)
    }
}