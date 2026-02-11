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
import kotlin.test.assertNull
import kotlin.time.ExperimentalTime

class EndPeriodUseCaseTest {

    private lateinit var mockRepository: PeriodRepository
    private lateinit var useCase: EndPeriodUseCase

    @BeforeTest
    fun setUp() {
        mockRepository = mockk<PeriodRepository>(relaxed = true)
        useCase = EndPeriodUseCase(mockRepository)
    }

    @Test
    fun invoke_WHEN_periodIdIsProvided_THEN_callsRepositoryEndPeriodWithCorrectId() = runTest {
        // ARRANGE
        val testEndDate = TestData.DATE
        val testPeriodId = "specific-period-id-456"

        // ACT
        useCase(endDate = testEndDate, periodId = testPeriodId)

        // ASSERT
        coVerify(exactly = 1) {
            mockRepository.endPeriod(periodId = testPeriodId, endDate = testEndDate)
        }
    }

    @OptIn(ExperimentalTime::class)
    @Test
    fun invoke_WHEN_periodIdIsNullAndOngoingPeriodExists_THEN_endsOngoingPeriod() = runTest {
        // ARRANGE
        val testEndDate = TestData.DATE
        val fakeOngoingPeriod = buildPeriod(
            id = "ongoing-period-id-123",
            startDate = testEndDate.minus(5, DateTimeUnit.DAY),
            endDate = null
        )
        coEvery { mockRepository.getCurrentlyOngoingPeriod() } returns fakeOngoingPeriod

        // ACT
        useCase(endDate = testEndDate, periodId = null)

        // ASSERT
        coVerify(exactly = 1) { mockRepository.getCurrentlyOngoingPeriod() }
        coVerify(exactly = 1) {
            mockRepository.endPeriod(periodId = fakeOngoingPeriod.id, endDate = testEndDate)
        }
    }

    @Test
    fun invoke_WHEN_periodIdIsNullAndNoOngoingPeriodExists_THEN_doesNothingAndReturnsNull() = runTest {
        // ARRANGE
        val testEndDate = TestData.DATE
        coEvery { mockRepository.getCurrentlyOngoingPeriod() } returns null

        // ACT
        val result = useCase(endDate = testEndDate, periodId = null)

        // ASSERT
        assertNull(result)
        coVerify(exactly = 0) { mockRepository.endPeriod(any(), any()) }
    }
}
