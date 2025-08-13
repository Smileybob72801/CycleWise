package com.veleda.cyclewise.domain.usecases

import com.veleda.cyclewise.domain.models.DailyEntry
import com.veleda.cyclewise.domain.repository.CycleRepository
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.todayIn
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import com.benasher44.uuid.uuid4

/**
 * Fetches the DailyEntry for a given date. If none exists, it creates a new, empty one
 * linked to the currently ongoing cycle.
 */
@OptIn(ExperimentalTime::class)
class GetOrCreateDailyEntryUseCase(
    private val repository: CycleRepository
) {
    suspend operator fun invoke(date: LocalDate): DailyEntry? {
        // 1. Try to fetch an existing entry
        val existingEntry = repository.getEntryForDate(date)
        if (existingEntry != null) {
            return existingEntry
        }

        // 2. If no entry, find the currently active cycle to link to
        val ongoingCycle = repository.getCurrentlyOngoingCycle() ?: return null

        // 3. Create a new, blank entry for the date
        val now = Clock.System.now()
        val dayInCycle = date.day - ongoingCycle.startDate.day + 1 // Simplified calculation

        return DailyEntry(
            id = uuid4().toString(),
            cycleId = ongoingCycle.id,
            entryDate = date,
            dayInCycle = dayInCycle,
            createdAt = now,
            updatedAt = now
            // All other fields are default (null, false, emptyList)
        )
    }
}
