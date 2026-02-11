package com.veleda.cyclewise.domain.usecases

import com.veleda.cyclewise.domain.repository.PeriodRepository
import com.veleda.cyclewise.testutil.TestData
import com.veleda.cyclewise.testutil.buildPeriod
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.minus
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.time.ExperimentalTime

class StartNewCycleUseCaseTest {

    private lateinit var mockRepository: PeriodRepository
    private lateinit var useCase: StartNewPeriodUseCase

    @BeforeTest
    fun setUp() {
        mockRepository = mockk<PeriodRepository>(relaxed = true)
        useCase = StartNewPeriodUseCase(mockRepository)
    }

    @Test
    fun invoke_WHEN_noOngoingCycleExists_THEN_callsRepositoryStartNewCycle() = runTest {
        // ARRANGE
        val testStartDate = TestData.DATE
        coEvery { mockRepository.getCurrentlyOngoingPeriod() } returns null

        // ACT
        val result = useCase(startDate = testStartDate)

        // ASSERT
        assertNotNull(result)
        coVerify(exactly = 1) {
            mockRepository.startNewPeriod(startDate = testStartDate)
        }
    }

    @OptIn(ExperimentalTime::class)
    @Test
    fun invoke_WHEN_ongoingCycleExists_THEN_doesNothingAndReturnsNull() = runTest {
        // ARRANGE
        val testStartDate = TestData.DATE
        val fakeOngoingPeriod = buildPeriod(
            id = "ongoing-cycle-id-123",
            startDate = testStartDate.minus(5, DateTimeUnit.DAY),
            endDate = null
        )
        coEvery { mockRepository.getCurrentlyOngoingPeriod() } returns fakeOngoingPeriod

        // ACT
        val result = useCase(startDate = testStartDate)

        // ASSERT
        assertNull(result)
        coVerify(exactly = 0) { mockRepository.startNewPeriod(any()) }
    }
}
