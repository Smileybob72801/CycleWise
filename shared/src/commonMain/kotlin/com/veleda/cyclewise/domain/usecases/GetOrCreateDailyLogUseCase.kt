package com.veleda.cyclewise.domain.usecases

import com.benasher44.uuid.uuid4
import com.veleda.cyclewise.domain.models.DailyEntry
import com.veleda.cyclewise.domain.models.FullDailyLog
import com.veleda.cyclewise.domain.repository.PeriodRepository
import kotlinx.coroutines.flow.first
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Retrieves an existing [FullDailyLog] for the given date, or creates a new blank one.
 *
 * Parent period lookup: finds the first period (sorted by start date descending) whose
 * start date is on or before [date]. The [DailyEntry.dayInCycle] is calculated as
 * `parentPeriod.startDate.daysUntil(date) + 1` (1-based).
 *
 * When no parent period exists (e.g., fresh install with no period data), a blank
 * [FullDailyLog] is still returned with [DailyEntry.dayInCycle] set to `0` — the
 * sentinel value meaning "no parent period found at creation time."
 *
 * @return the existing or newly created [FullDailyLog]; never null.
 */
@OptIn(ExperimentalTime::class)
class GetOrCreateDailyLogUseCase(
    private val repository: PeriodRepository
) {
    /**
     * Returns the [FullDailyLog] for [date], creating a blank one if none exists.
     *
     * A new entry's [DailyEntry.dayInCycle] is calculated relative to the most
     * recent period's start date (1-based), or set to `0` when no parent period
     * is found.
     *
     * @param date The calendar date to retrieve or create a log for.
     * @return The existing or newly created [FullDailyLog].
     */
    suspend operator fun invoke(date: LocalDate): FullDailyLog {
        val existingLog = repository.getFullLogForDate(date)
        if (existingLog != null) {
            return existingLog
        }

        val parentPeriod = repository.getAllPeriods().first()
            .firstOrNull { it.startDate <= date }

        val dayInCycle = parentPeriod?.let { it.startDate.daysUntil(date) + 1 } ?: 0
        val newBlankEntry = DailyEntry(
            id = uuid4().toString(),
            entryDate = date,
            dayInCycle = dayInCycle,
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )
        return FullDailyLog(entry = newBlankEntry)
    }
}