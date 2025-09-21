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
import kotlin.test.assertNull
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class EndCycleUseCaseTest {

    private lateinit var mockRepository: CycleRepository
    private lateinit var useCase: EndCycleUseCase

    @BeforeTest
    fun setUp() {
        // Use relaxed = true because we often don't care about the return value,
        // just that the correct repository method was called.
        mockRepository = mockk<CycleRepository>(relaxed = true)
        useCase = EndCycleUseCase(mockRepository)
    }

    @OptIn(ExperimentalTime::class)
    @Test
    fun invoke_WHEN_cycleIdIsProvided_THEN_callsRepositoryEndCycleWithCorrectId() {
        runTest {
            // --- ARRANGE ---
            val testEndDate = Clock.System.todayIn(TimeZone.currentSystemDefault())
            val testCycleId = "specific-cycle-id-456"

            // --- ACT ---
            useCase(endDate = testEndDate, cycleId = testCycleId)

            // --- ASSERT ---
            // Verify that the repository's `endCycle` method was called exactly
            // once with the specific arguments we provided.
            coVerify(exactly = 1) {
                mockRepository.endCycle(cycleId = testCycleId, endDate = testEndDate)
            }
        }
    }

    @OptIn(ExperimentalTime::class)
    @Test
    fun invoke_WHEN_cycleIdIsNullAndOngoingCycleExists_THEN_endsOngoingCycle() {
        runTest {
            // --- ARRANGE ---
            val testEndDate = Clock.System.todayIn(TimeZone.currentSystemDefault())
            val fakeOngoingCycle = Cycle(
                id = "ongoing-cycle-id-123",
                startDate = testEndDate.minus(5, DateTimeUnit.DAY),
                endDate = null,
                createdAt = Clock.System.now(),
                updatedAt = Clock.System.now()
            )

            // Mock the two repository calls that will be made
            coEvery { mockRepository.getCurrentlyOngoingCycle() } returns fakeOngoingCycle

            // --- ACT ---
            useCase(endDate = testEndDate, cycleId = null)

            // --- ASSERT ---
            // 1. Verify that we first checked for an ongoing cycle.
            coVerify(exactly = 1) { mockRepository.getCurrentlyOngoingCycle() }

            // 2. Verify that `endCycle` was then called with the ID from our fake cycle.
            coVerify(exactly = 1) {
                mockRepository.endCycle(cycleId = fakeOngoingCycle.id, endDate = testEndDate)
            }
        }
    }

    @OptIn(ExperimentalTime::class)
    @Test
    fun invoke_WHEN_cycleIdIsNullAndNoOngoingCycleExists_THEN_doesNothingAndReturnsNull() {
        runTest {
            // --- ARRANGE ---
            val testEndDate = Clock.System.todayIn(TimeZone.currentSystemDefault())

            // Mock the repository to return `null` for the ongoing cycle check.
            coEvery { mockRepository.getCurrentlyOngoingCycle() } returns null

            // --- ACT ---
            val result = useCase(endDate = testEndDate, cycleId = null)

            // --- ASSERT ---
            // 1. Assert that the use case correctly returned null.
            assertNull(result)

            // 2. Crucially, verify that `endCycle` was NEVER called.
            coVerify(exactly = 0) { mockRepository.endCycle(any(), any()) }
        }
    }
}