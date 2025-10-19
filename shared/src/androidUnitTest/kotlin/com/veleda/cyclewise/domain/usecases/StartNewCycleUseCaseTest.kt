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
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class StartNewCycleUseCaseTest {

    private lateinit var mockRepository: PeriodRepository
    private lateinit var useCase: StartNewPeriodUseCase

    @BeforeTest
    fun setUp() {
        mockRepository = mockk<PeriodRepository>(relaxed = true)
        useCase = StartNewPeriodUseCase(mockRepository)
    }

    @OptIn(ExperimentalTime::class)
    @Test
    fun invoke_WHEN_noOngoingCycleExists_THEN_callsRepositoryStartNewCycle() {
        runTest {
            // --- ARRANGE ---
            val testStartDate = Clock.System.todayIn(TimeZone.currentSystemDefault())

            // Mock the repository to return `null`, indicating no cycle is currently ongoing.
            coEvery { mockRepository.getCurrentlyOngoingPeriod() } returns null

            // --- ACT ---
            val result = useCase(startDate = testStartDate)

            // --- ASSERT ---
            // 1. Assert that the use case did not fail.
            assertNotNull(result)

            // 2. Verify that the repository's `startNewPeriod` method was called
            //    exactly once with the correct start date.
            coVerify(exactly = 1) {
                mockRepository.startNewPeriod(startDate = testStartDate)
            }
        }
    }

    @OptIn(ExperimentalTime::class)
    @Test
    fun invoke_WHEN_ongoingCycleExists_THEN_doesNothingAndReturnsNull() {
        runTest {
            // --- ARRANGE ---
            val testStartDate = Clock.System.todayIn(TimeZone.currentSystemDefault())
            val fakeOngoingPeriod = Period(
                id = "ongoing-cycle-id-123",
                startDate = testStartDate.minus(5, DateTimeUnit.DAY),
                endDate = null,
                createdAt = Clock.System.now(),
                updatedAt = Clock.System.now()
            )

            // Mock the repository to return a valid, existing ongoing cycle.
            coEvery { mockRepository.getCurrentlyOngoingPeriod() } returns fakeOngoingPeriod

            // --- ACT ---
            val result = useCase(startDate = testStartDate)

            // --- ASSERT ---
            // 1. Assert that the use case correctly returned null to indicate failure.
            assertNull(result)

            // 2. Verify that `startNewPeriod` was NEVER called.
            coVerify(exactly = 0) { mockRepository.startNewPeriod(any()) }
        }
    }
}