package com.veleda.cyclewise.domain.usecases

import com.veleda.cyclewise.domain.models.Period
import com.veleda.cyclewise.domain.repository.PeriodRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.*
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertNull
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class EndPeriodUseCaseTest {

    private lateinit var mockRepository: PeriodRepository
    private lateinit var useCase: EndPeriodUseCase

    @BeforeTest
    fun setUp() {
        // Use relaxed = true because we often don't care about the return value,
        // just that the correct repository method was called.
        mockRepository = mockk<PeriodRepository>(relaxed = true)
        useCase = EndPeriodUseCase(mockRepository)
    }

    @OptIn(ExperimentalTime::class)
    @Test
    fun invoke_WHEN_periodIdIsProvided_THEN_callsRepositoryEndPeriodWithCorrectId() {
        runTest {
            // --- ARRANGE ---
            val testEndDate = Clock.System.todayIn(TimeZone.currentSystemDefault())
            val testPeriodId = "specific-period-id-456"

            // --- ACT ---
            useCase(endDate = testEndDate, periodId = testPeriodId)

            // --- ASSERT ---
            // Verify that the repository's `endPeriod` method was called exactly
            // once with the specific arguments we provided.
            coVerify(exactly = 1) {
                mockRepository.endPeriod(periodId = testPeriodId, endDate = testEndDate)
            }
        }
    }

    @OptIn(ExperimentalTime::class)
    @Test
    fun invoke_WHEN_periodIdIsNullAndOngoingPeriodExists_THEN_endsOngoingPeriod() {
        runTest {
            // --- ARRANGE ---
            val testEndDate = Clock.System.todayIn(TimeZone.currentSystemDefault())
            val fakeOngoingPeriod = Period(
                id = "ongoing-period-id-123",
                startDate = testEndDate.minus(5, DateTimeUnit.DAY),
                endDate = null,
                createdAt = Clock.System.now(),
                updatedAt = Clock.System.now()
            )

            // Mock the two repository calls that will be made
            coEvery { mockRepository.getCurrentlyOngoingPeriod() } returns fakeOngoingPeriod

            // --- ACT ---
            useCase(endDate = testEndDate, periodId = null)

            // --- ASSERT ---
            // 1. Verify that we first checked for an ongoing period.
            coVerify(exactly = 1) { mockRepository.getCurrentlyOngoingPeriod() }

            // 2. Verify that `endPeriod` was then called with the ID from our fake period.
            coVerify(exactly = 1) {
                mockRepository.endPeriod(periodId = fakeOngoingPeriod.id, endDate = testEndDate)
            }
        }
    }

    @OptIn(ExperimentalTime::class)
    @Test
    fun invoke_WHEN_periodIdIsNullAndNoOngoingPeriodExists_THEN_doesNothingAndReturnsNull() {
        runTest {
            // --- ARRANGE ---
            val testEndDate = Clock.System.todayIn(TimeZone.currentSystemDefault())

            // Mock the repository to return `null` for the ongoing period check.
            coEvery { mockRepository.getCurrentlyOngoingPeriod() } returns null

            // --- ACT ---
            val result = useCase(endDate = testEndDate, periodId = null)

            // --- ASSERT ---
            // 1. Assert that the use case correctly returned null.
            assertNull(result)

            // 2. Crucially, verify that `endPeriod` was NEVER called.
            coVerify(exactly = 0) { mockRepository.endPeriod(any(), any()) }
        }
    }
}