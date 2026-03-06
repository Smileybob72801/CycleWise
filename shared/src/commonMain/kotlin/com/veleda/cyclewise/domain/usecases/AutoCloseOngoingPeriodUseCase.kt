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
 * Automatically closes the ongoing period when a 1-day gap is detected.
 *
 * Triggered by [TrackerEvent.ScreenEntered] each time the tracker screen appears.
 * Compares today's date against the last logged period day within the ongoing period.
 * If the day after the last logged day is today or earlier, the period is closed with
 * its end date set to the last logged day.
 *
 * **No-op** when no ongoing period exists or when the last logged day is today.
 */
class AutoCloseOngoingPeriodUseCase(
    private val repository: PeriodRepository
) {
    /**
     * Checks for an ongoing period and closes it if a 1-day gap is detected.
     *
     * Compares today's date against the last logged period day. If the day
     * after the last log is today or earlier, the period's end date is set
     * to that last logged day. No-op when no ongoing period exists or the
     * last logged day is today.
     */
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