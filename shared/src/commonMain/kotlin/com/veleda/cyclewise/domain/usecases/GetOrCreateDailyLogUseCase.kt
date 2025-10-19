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

@OptIn(ExperimentalTime::class)
class GetOrCreateDailyLogUseCase(
    private val repository: PeriodRepository
) {
    suspend operator fun invoke(date: LocalDate): FullDailyLog? {
        // 1. Try to fetch an existing log.
        val existingLog = repository.getFullLogForDate(date)
        if (existingLog != null) {
            return existingLog
        }

        // 2. If no log exists, find the parent period to calculate dayInCycle.
        val parentPeriod = repository.getAllPeriods().first()
            .firstOrNull { it.startDate <= date }

        // If no periods exist at all, we can't calculate dayInCycle.
        // We could create a log with dayInCycle = 0, but I'm choosing to enforce
        // that at least one period must exist before logging.
        // TODO: Remove day in cycle, this no longer makes sense. We will infer this.
        parentPeriod ?: return null

        // 3. Create a new, blank log.
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