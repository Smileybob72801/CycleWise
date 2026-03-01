package com.veleda.cyclewise.domain.usecases

import com.veleda.cyclewise.domain.models.DailyEntry
import com.veleda.cyclewise.domain.models.FullDailyLog
import com.veleda.cyclewise.domain.repository.PeriodRepository
import com.veleda.cyclewise.testutil.TestData
import com.veleda.cyclewise.testutil.buildPeriod
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.time.ExperimentalTime

class GetOrCreateDailyLogUseCaseTest {

    private lateinit var mockRepository: PeriodRepository
    private lateinit var useCase: GetOrCreateDailyLogUseCase

    // --- Test Data Setup ---
    @OptIn(ExperimentalTime::class)
    private val testPeriods = listOf(
        buildPeriod(id = "period-1", startDate = LocalDate(2025, 1, 1), endDate = LocalDate(2025, 1, 5)),
        buildPeriod(id = "period-2", startDate = LocalDate(2025, 1, 29), endDate = LocalDate(2025, 2, 2)),
        buildPeriod(id = "period-3", startDate = LocalDate(2025, 2, 28), endDate = LocalDate(2025, 3, 4))
    )

    @Before
    fun setUp() {
        mockRepository = mockk()
        useCase = GetOrCreateDailyLogUseCase(mockRepository)
        coEvery { mockRepository.getAllPeriods() } returns flowOf(testPeriods.reversed())
    }

    @OptIn(ExperimentalTime::class)
    @Test
    fun invoke_WHEN_logExists_THEN_returnsExistingLog() = runTest {
        // ARRANGE
        val testDate = LocalDate(2025, 1, 15)
        val existingEntry = DailyEntry(
            id = "existing-id",
            entryDate = testDate,
            dayInCycle = 15,
            createdAt = TestData.INSTANT,
            updatedAt = TestData.INSTANT
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
    fun invoke_WHEN_noLogExists_THEN_linksToCorrectParentPeriod() = runTest {
        // ARRANGE
        val testDate = LocalDate(2025, 2, 10)
        coEvery { mockRepository.getFullLogForDate(testDate) } returns null
        val parentPeriod = testPeriods[1]
        val expectedDayInCycle = parentPeriod.startDate.daysUntil(testDate) + 1

        // ACT
        val result = useCase(testDate)

        // ASSERT
        assertNotNull(result)
        assertEquals(testDate, result.entry.entryDate)
        assertEquals(expectedDayInCycle, result.entry.dayInCycle, "Day in cycle should be calculated from the correct parent period")
        assertEquals(13, expectedDayInCycle)
    }

    @Test
    fun invoke_WHEN_dateIsStartOfPeriod_THEN_dayInCycleIsOne() = runTest {
        // ARRANGE
        val testDate = LocalDate(2025, 1, 29)
        coEvery { mockRepository.getFullLogForDate(testDate) } returns null

        // ACT
        val result = useCase(testDate)

        // ASSERT
        assertNotNull(result)
        assertEquals(1, result.entry.dayInCycle, "Day in cycle should be 1 on the first day of a period")
    }

    @Test
    fun invoke_WHEN_dateBeforeFirstPeriod_THEN_returnsDayInCycleZero() = runTest {
        // ARRANGE
        val testDate = LocalDate(2024, 12, 31)
        coEvery { mockRepository.getFullLogForDate(testDate) } returns null

        // ACT
        val result = useCase(testDate)

        // ASSERT
        assertNotNull(result, "Should return a FullDailyLog even when no parent period precedes the date")
        assertEquals(0, result.entry.dayInCycle, "dayInCycle should be 0 (sentinel) when no parent period exists")
        assertEquals(testDate, result.entry.entryDate)
    }

    @Test
    fun invoke_WHEN_noPeriodsExist_THEN_returnsDayInCycleZero() = runTest {
        // ARRANGE
        val testDate = LocalDate(2025, 1, 15)
        coEvery { mockRepository.getFullLogForDate(testDate) } returns null
        coEvery { mockRepository.getAllPeriods() } returns flowOf(emptyList())

        // ACT
        val result = useCase(testDate)

        // ASSERT
        assertNotNull(result, "Should return a FullDailyLog even when the database is completely empty")
        assertEquals(0, result.entry.dayInCycle, "dayInCycle should be 0 (sentinel) when no periods exist")
        assertEquals(testDate, result.entry.entryDate)
    }
}
