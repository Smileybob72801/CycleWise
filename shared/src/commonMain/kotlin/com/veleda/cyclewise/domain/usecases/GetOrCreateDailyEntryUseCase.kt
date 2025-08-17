package com.veleda.cyclewise.domain.usecases

import com.benasher44.uuid.uuid4
import com.veleda.cyclewise.domain.models.DailyEntry
import com.veleda.cyclewise.domain.repository.CycleRepository
import kotlin.time.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil
import kotlin.time.ExperimentalTime

/**
 * Fetches the DailyEntry for a given date. If none exists, it creates a new, empty one
 * linked to the currently ongoing cycle.
 */
@OptIn(ExperimentalTime::class)
class GetOrCreateDailyEntryUseCase(
    private val repository: CycleRepository
) {
    suspend operator fun invoke(date: LocalDate): DailyEntry? {
        // 1. Try to fetch an existing full log using the new repository method.
        val existingFullLog = repository.getFullLogForDate(date)
        if (existingFullLog != null) {
            // An entry already exists, so return it.
            return existingFullLog.entry
        }

        // 2. If no entry exists, find the currently active cycle to link to.
        val ongoingCycle = repository.getCurrentlyOngoingCycle() ?: return null

        // 3. Create a new, blank entry for the date.
        val now = Clock.System.now()
        // A more robust day-in-cycle calculation
        val dayInCycle = ongoingCycle.startDate.daysUntil(date) + 1

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