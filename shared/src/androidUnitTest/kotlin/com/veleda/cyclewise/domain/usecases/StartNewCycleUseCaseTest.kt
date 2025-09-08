package com.veleda.cyclewise.domain.usecases

import com.veleda.cyclewise.domain.models.Cycle
import com.veleda.cyclewise.domain.repository.CycleRepository
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

    private lateinit var mockRepository: CycleRepository
    private lateinit var useCase: StartNewCycleUseCase

    @BeforeTest
    fun setUp() {
        mockRepository = mockk<CycleRepository>(relaxed = true)
        useCase = StartNewCycleUseCase(mockRepository)
    }

    @OptIn(ExperimentalTime::class)
    @Test
    fun invoke_WHEN_noOngoingCycleExists_THEN_callsRepositoryStartNewCycle() {
        runTest {
            // --- ARRANGE ---
            val testStartDate = Clock.System.todayIn(TimeZone.currentSystemDefault())

            // Mock the repository to return `null`, indicating no cycle is currently ongoing.
            coEvery { mockRepository.getCurrentlyOngoingCycle() } returns null

            // --- ACT ---
            val result = useCase(startDate = testStartDate)

            // --- ASSERT ---
            // 1. Assert that the use case did not fail.
            assertNotNull(result)

            // 2. Verify that the repository's `startNewCycle` method was called
            //    exactly once with the correct start date.
            coVerify(exactly = 1) {
                mockRepository.startNewCycle(startDate = testStartDate)
            }
        }
    }

    @OptIn(ExperimentalTime::class)
    @Test
    fun invoke_WHEN_ongoingCycleExists_THEN_doesNothingAndReturnsNull() {
        runTest {
            // --- ARRANGE ---
            val testStartDate = Clock.System.todayIn(TimeZone.currentSystemDefault())
            val fakeOngoingCycle = Cycle(
                id = "ongoing-cycle-id-123",
                startDate = testStartDate.minus(5, DateTimeUnit.DAY),
                endDate = null,
                createdAt = Clock.System.now(),
                updatedAt = Clock.System.now()
            )

            // Mock the repository to return a valid, existing ongoing cycle.
            coEvery { mockRepository.getCurrentlyOngoingCycle() } returns fakeOngoingCycle

            // --- ACT ---
            val result = useCase(startDate = testStartDate)

            // --- ASSERT ---
            // 1. Assert that the use case correctly returned null to indicate failure.
            assertNull(result)

            // 2. Verify that `startNewCycle` was NEVER called.
            coVerify(exactly = 0) { mockRepository.startNewCycle(any()) }
        }
    }
}