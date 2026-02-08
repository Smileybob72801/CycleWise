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
 * Retrieves an existing [FullDailyLog] for the given date, or creates a new blank one
 * if a parent period exists. Returns null when no period context is available.
 */
@OptIn(ExperimentalTime::class)
class GetOrCreateDailyLogUseCase(
    private val repository: PeriodRepository
) {
    suspend operator fun invoke(date: LocalDate): FullDailyLog? {
        val existingLog = repository.getFullLogForDate(date)
        if (existingLog != null) {
            return existingLog
        }

        val parentPeriod = repository.getAllPeriods().first()
            .firstOrNull { it.startDate <= date }

        parentPeriod ?: return null

        val dayInCycle = parentPeriod.startDate.daysUntil(date) + 1
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