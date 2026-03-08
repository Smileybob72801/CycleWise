package com.veleda.cyclewise.domain.usecases

import com.veleda.cyclewise.domain.repository.PeriodRepository
import kotlinx.datetime.LocalDate

/**
 * Deletes tutorial seed data identified by a [SeedManifest].
 *
 * Called after the tutorial walkthrough completes (normal completion, skip, or safety-net
 * on next DailyLogScreen load). Delegates to [PeriodRepository.deleteSeedData] which
 * runs inside a single database transaction.
 *
 * Only the exact record IDs listed in the manifest are deleted — real user data is
 * never affected.
 *
 * @param repository The data access contract for deleting seed records.
 */
class TutorialCleanupUseCase(
    private val repository: PeriodRepository,
) {

    /**
     * Deletes all seed data described by [manifest].
     *
     * Parses the ISO-8601 water intake date strings into [LocalDate] and forwards
     * them to the repository for transactional deletion.
     */
    suspend operator fun invoke(manifest: SeedManifest) {
        repository.deleteSeedData(
            periodUuids = manifest.periodUuids,
            entryIds = manifest.dailyEntryIds,
            waterDates = manifest.waterIntakeDates.map { LocalDate.parse(it) },
        )
    }
}
