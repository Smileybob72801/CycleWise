package com.veleda.cyclewise.domain.usecases

import com.veleda.cyclewise.domain.repository.PeriodRepository
import kotlinx.coroutines.flow.first
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Automatically checks the ongoing period and closes it if there is a 1-day gap
 * since the last recorded period day.
 */
class AutoCloseOngoingPeriodUseCase(
    private val repository: PeriodRepository
) {
    @OptIn(ExperimentalTime::class)
    suspend operator fun invoke() {
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())

        val ongoingPeriod = repository.getCurrentlyOngoingPeriod() ?: return

        val allPeriodDays = repository.observeAllPeriodDays().first()

        val lastLoggedPeriodDay = allPeriodDays
            .filter { it >= ongoingPeriod.startDate }
            .maxOrNull() ?: ongoingPeriod.startDate

        val dateToCheck = lastLoggedPeriodDay.plus(1, DateTimeUnit.DAY)

        if (dateToCheck <= today) {
            val closingDate = lastLoggedPeriodDay
            repository.updatePeriodEndDate(ongoingPeriod.id, closingDate)
        }
    }
}