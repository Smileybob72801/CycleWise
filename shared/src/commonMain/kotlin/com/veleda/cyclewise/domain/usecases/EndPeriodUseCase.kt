package com.veleda.cyclewise.domain.usecases

import com.veleda.cyclewise.domain.repository.PeriodRepository
import kotlinx.datetime.LocalDate

class EndPeriodUseCase(
    private val repository: PeriodRepository
) {
    /**
     * End the ongoing cycle (or a specific one) at [endDate].
     * If [periodId] is null, it will end the currently ongoing cycle if present.
     * @return the updated cycle, or null if nothing to end.
     */
    suspend operator fun invoke(endDate: LocalDate, periodId: String? = null) =
        if (periodId != null) repository.endPeriod(periodId, endDate)
        else repository.getCurrentlyOngoingPeriod()?.let { c -> repository.endPeriod(c.id, endDate) }
}