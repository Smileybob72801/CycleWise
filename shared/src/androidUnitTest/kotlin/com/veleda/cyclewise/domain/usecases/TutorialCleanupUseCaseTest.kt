package com.veleda.cyclewise.domain.usecases

import com.veleda.cyclewise.domain.repository.PeriodRepository
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [TutorialCleanupUseCase].
 *
 * Verifies that the use case correctly forwards the [SeedManifest] data to
 * [PeriodRepository.deleteSeedData] with properly parsed arguments.
 */
class TutorialCleanupUseCaseTest {

    private lateinit var repository: PeriodRepository
    private lateinit var useCase: TutorialCleanupUseCase

    @Before
    fun setUp() {
        repository = mockk(relaxed = true)
        useCase = TutorialCleanupUseCase(repository)
    }

    @Test
    fun `invoke WHEN givenManifest THEN callsDeleteSeedDataWithCorrectArgs`() = runTest {
        // GIVEN
        val manifest = SeedManifest(
            periodUuids = listOf("uuid-1", "uuid-2", "uuid-3"),
            dailyEntryIds = listOf("entry-a", "entry-b"),
            waterIntakeDates = listOf("2026-01-10", "2026-01-11"),
        )

        // WHEN
        useCase(manifest)

        // THEN
        coVerify(exactly = 1) {
            repository.deleteSeedData(
                periodUuids = listOf("uuid-1", "uuid-2", "uuid-3"),
                entryIds = listOf("entry-a", "entry-b"),
                waterDates = listOf(
                    LocalDate(2026, 1, 10),
                    LocalDate(2026, 1, 11),
                ),
            )
        }
    }

    @Test
    fun `invoke WHEN emptyManifest THEN callsDeleteSeedDataWithEmptyLists`() = runTest {
        // GIVEN
        val manifest = SeedManifest(
            periodUuids = emptyList(),
            dailyEntryIds = emptyList(),
            waterIntakeDates = emptyList(),
        )

        // WHEN
        useCase(manifest)

        // THEN
        coVerify(exactly = 1) {
            repository.deleteSeedData(
                periodUuids = emptyList(),
                entryIds = emptyList(),
                waterDates = emptyList(),
            )
        }
    }
}
