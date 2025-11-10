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

        // Find the last actual day that was part of a period.
        val allPeriodDays = repository.observeAllPeriodDays().first()

        // The ongoing period's start date is the earliest period day.
        // The *actual* last period day might be earlier than the current date.
        val lastLoggedPeriodDay = allPeriodDays
            .filter { it >= ongoingPeriod.startDate } // Only consider days *after* the start
            .maxOrNull() ?: ongoingPeriod.startDate

        // If there is at least a one day gap since the last period day, I consider the period closed.
        // Last period day was yesterday or earlier.
        val dateToCheck = lastLoggedPeriodDay.plus(1, DateTimeUnit.DAY)

        // If the date to check is before or on today, it means the period has been over for at least a day.
        if (dateToCheck <= today) {
            // The closure date is the day *before* the first gap.
            val closingDate = lastLoggedPeriodDay
            repository.updatePeriodEndDate(ongoingPeriod.id, closingDate)
        }
    }
}