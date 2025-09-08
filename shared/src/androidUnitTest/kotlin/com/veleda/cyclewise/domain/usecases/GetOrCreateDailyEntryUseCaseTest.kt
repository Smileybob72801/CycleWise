package com.veleda.cyclewise.domain.usecases

import com.veleda.cyclewise.domain.models.Cycle
import com.veleda.cyclewise.domain.models.DailyEntry
import com.veleda.cyclewise.domain.models.FullDailyLog
import com.veleda.cyclewise.domain.repository.CycleRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import org.junit.Before
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import org.junit.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.minus
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull

class GetOrCreateDailyEntryUseCaseTest {

    // --- 1. SETUP ---

    private lateinit var mockRepository: CycleRepository
    private lateinit var useCase: GetOrCreateDailyEntryUseCase

    @Before
    fun setUp() {
        mockRepository = mockk<CycleRepository>()

        useCase = GetOrCreateDailyEntryUseCase(mockRepository)
    }

    @OptIn(ExperimentalTime::class)
    @Test
    fun invoke_WHEN_RepositoryReturnsExisting_THEN_Log_returnsCorrectDailyEntry() {
        runTest {
            // --- ARRANGE ---
            val testDate = Clock.System.todayIn(TimeZone.currentSystemDefault())

            val expectedEntry = DailyEntry(
                id = "test-entry-id",
                cycleId = "test-cycle-id",
                entryDate = testDate,
                dayInCycle = 1,
                createdAt = Clock.System.now(),
                updatedAt = Clock.System.now()
            )

            val fakeLog = FullDailyLog(entry = expectedEntry)
            coEvery { mockRepository.getFullLogForDate(any()) } returns fakeLog

            // --- ACT ---
            val result = useCase(testDate)

            // --- ASSERT ---
            assertEquals(expectedEntry, result)
        }
    }

    @OptIn(ExperimentalTime::class)
    @Test
    fun invoke_WHEN_no_log_exists_THEN_creates_and_returns_a_new_DailyEntry () {
        runTest {
            // --- ARRANGE ---
            val testDate = Clock.System.todayIn(TimeZone.currentSystemDefault())
            val fakeOngoingCycle = Cycle(
                id = "ongoing-cycle-id-123",
                startDate = testDate.minus(5, DateTimeUnit.DAY), // Cycle started 5 days ago
                endDate = null,
                createdAt = Clock.System.now(),
                updatedAt = Clock.System.now()
            )

            // We tell the mock repository two things:
            // 1. There is no existing log for this date.
            coEvery { mockRepository.getFullLogForDate(any()) } returns null
            // 2. There IS a currently ongoing cycle to link the new log to.
            coEvery { mockRepository.getCurrentlyOngoingCycle() } returns fakeOngoingCycle

            // --- ACT ---
            val result = useCase(testDate)

            // --- ASSERT ---
            // 1. Assert that the result is not null (a new entry was created).
            assertNotNull(result)
            // 2. Assert that the new entry is for the correct date.
            assertEquals(testDate, result?.entryDate)
            // 3. Assert that the new entry is correctly linked to the ongoing cycle.
            assertEquals(fakeOngoingCycle.id, result?.cycleId)
            // 4. Assert the day in cycle is calculated correctly (today is day 6 of the cycle).
            assertEquals(6, result?.dayInCycle)
        }
    }

    @OptIn(ExperimentalTime::class)
    @Test
    fun invoke_WHEN_noOngoingCycle_THEN_returnsNull() {
        runTest {
            // --- ARRANGE ---
            val testDate = Clock.System.todayIn(TimeZone.currentSystemDefault())

            // We tell the mock repository two things:
            // 1. There is no existing log for this date.
            coEvery { mockRepository.getFullLogForDate(any()) } returns null
            // 2. Crucially, there is NO ongoing cycle. This simulates the app's state
            //    before the very first cycle has been started.
            coEvery { mockRepository.getCurrentlyOngoingCycle() } returns null

            // --- ACT ---
            // Try to create a log entry when it's impossible to link it to a cycle.
            val result = useCase(testDate)

            // --- ASSERT ---
            // The use case should fail gracefully by returning null.
            assertNull(result)
        }
    }
}