package com.veleda.cyclewise.domain.usecases

import com.veleda.cyclewise.domain.models.DailyEntry
import com.veleda.cyclewise.domain.models.FullDailyLog
import com.veleda.cyclewise.domain.models.Period
import com.veleda.cyclewise.domain.repository.PeriodRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.time.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.time.ExperimentalTime

class GetOrCreateDailyLogUseCaseTest {

    private lateinit var mockRepository: PeriodRepository
    private lateinit var useCase: GetOrCreateDailyLogUseCase

    // --- Test Data Setup ---
    // A stable, predictable set of periods for all tests. Oldest first.
    @OptIn(ExperimentalTime::class)
    private val testPeriods = listOf(
        Period("period-1", LocalDate(2025, 1, 1), LocalDate(2025, 1, 5), Clock.System.now(), Clock.System.now()),
        Period("period-2", LocalDate(2025, 1, 29), LocalDate(2025, 2, 2), Clock.System.now(), Clock.System.now()), // 28-day cycle
        Period("period-3", LocalDate(2025, 2, 28), LocalDate(2025, 3, 4), Clock.System.now(), Clock.System.now())  // 30-day cycle
    )

    @Before
    fun setUp() {
        mockRepository = mockk()
        useCase = GetOrCreateDailyLogUseCase(mockRepository)

        // Mock the repository to return our stable list of periods by default for all tests.
        coEvery { mockRepository.getAllPeriods() } returns flowOf(testPeriods.reversed()) // Repository returns newest first
    }

    @OptIn(ExperimentalTime::class)
    @Test
    fun invoke_whenLogAlreadyExists_returnsTheExistingLog() = runTest {
        // ARRANGE
        val testDate = LocalDate(2025, 1, 15)
        val existingEntry = DailyEntry(
            id = "existing-id",
            entryDate = testDate,
            dayInCycle = 15,
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )
        val existingLog = FullDailyLog(entry = existingEntry)
        coEvery { mockRepository.getFullLogForDate(testDate) } returns existingLog

        // ACT
        val result = useCase(testDate)

        // ASSERT
        assertNotNull(result)
        assertEquals(existingLog, result, "Should return the exact log provided by the repository")
    }

    @Test
    fun invoke_whenNoLogExists_createsNewLogLinkedToCorrectParentPeriod() = runTest {
        // ARRANGE
        // This date falls into the cycle that started on Jan 29.
        val testDate = LocalDate(2025, 2, 10)
        coEvery { mockRepository.getFullLogForDate(testDate) } returns null

        // The parent period should be period-2, which started on 2025-01-29.
        val parentPeriod = testPeriods[1]
        val expectedDayInCycle = parentPeriod.startDate.daysUntil(testDate) + 1

        // ACT
        val result = useCase(testDate)

        // ASSERT
        assertNotNull(result)
        assertEquals(testDate, result.entry.entryDate)
        // This is the most crucial assertion: it proves the use case correctly identified
        // the parent period to calculate the day of the cycle.
        assertEquals(expectedDayInCycle, result.entry.dayInCycle, "Day in cycle should be calculated from the correct parent period")
        assertEquals(13, expectedDayInCycle) // Explicitly check the math for this test case
    }

    @Test
    fun invoke_whenDateIsStartOfAPeriod_createsNewLogWithDayInCycleOne() = runTest {
        // ARRANGE
        // This date is the exact start of the second period.
        val testDate = LocalDate(2025, 1, 29)
        coEvery { mockRepository.getFullLogForDate(testDate) } returns null

        // ACT
        val result = useCase(testDate)

        // ASSERT
        assertNotNull(result)
        assertEquals(1, result.entry.dayInCycle, "Day in cycle should be 1 on the first day of a period")
    }

    @Test
    fun invoke_whenDateIsBeforeFirstEverPeriod_returnsNull() = runTest {
        // ARRANGE
        // A date that occurs before any logged periods in the repository.
        val testDate = LocalDate(2024, 12, 31)
        coEvery { mockRepository.getFullLogForDate(testDate) } returns null

        // ACT
        val result = useCase(testDate)

        // ASSERT
        assertNull(result, "Should return null because no parent period exists for a date before the first logged period")
    }

    @Test
    fun invoke_whenNoPeriodsExistAtAll_returnsNull() = runTest {
        // ARRANGE
        val testDate = LocalDate(2025, 1, 15)
        coEvery { mockRepository.getFullLogForDate(testDate) } returns null
        coEvery { mockRepository.getAllPeriods() } returns flowOf(emptyList()) // Override default setup

        // ACT
        val result = useCase(testDate)

        // ASSERT
        assertNull(result, "Should return null if the database is completely empty")
    }
}