package com.veleda.cyclewise.domain.usecases

import com.benasher44.uuid.uuid4
import com.veleda.cyclewise.domain.models.Period
import com.veleda.cyclewise.domain.repository.PeriodRepository
import kotlinx.datetime.LocalDate
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Starts a new menstrual period at the given [startDate].
 * Fails if there's already an ongoing period.
 */
class StartNewPeriodUseCase (private val repository: PeriodRepository)
{
    /**
     * Attempts to start a new period.
     * @return the new period, or null if a period is already ongoing
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