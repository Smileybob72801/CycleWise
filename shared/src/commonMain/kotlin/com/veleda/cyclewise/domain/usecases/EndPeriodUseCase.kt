package com.veleda.cyclewise.domain.usecases

import com.veleda.cyclewise.domain.repository.PeriodRepository
import kotlinx.datetime.LocalDate

/**
 * Ends a menstrual period by setting its end date.
 *
 * Supports two modes:
 * - **Explicit:** Pass a [periodId] to end a specific period.
 * - **Auto-find:** Pass null for [periodId] to end the currently ongoing period (if any).
 */
class EndPeriodUseCase(
    private val repository: PeriodRepository
) {
    /**
     * @param endDate   the date to set as the period's end date.
     * @param periodId  UUID of the period to end, or null to auto-find the ongoing period.
     * @return the updated [Period], or null if no matching period was found.
     */
    suspend operator fun invoke(endDate: LocalDate, periodId: String? = null) =
        if (periodId != null) repository.endPeriod(periodId, endDate)
        else repository.getCurrentlyOngoingPeriod()?.let { c -> repository.endPeriod(c.id, endDate) }
}