package com.veleda.cyclewise.domain.usecases

import com.veleda.cyclewise.domain.repository.CycleRepository
import kotlinx.datetime.LocalDate

class EndCycleUseCase(
    private val repository: CycleRepository
) {
    /**
     * End the ongoing cycle (or a specific one) at [endDate].
     * If [cycleId] is null, it will end the currently ongoing cycle if present.
     * @return the updated cycle, or null if nothing to end.
     */
    suspend operator fun invoke(endDate: LocalDate, cycleId: String? = null) =
        if (cycleId != null) repository.endCycle(cycleId, endDate)
        else repository.getCurrentlyOngoingCycle()?.let { c -> repository.endCycle(c.id, endDate) }
}