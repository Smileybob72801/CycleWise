package com.veleda.cyclewise.domain.usecases

import com.veleda.cyclewise.domain.repository.PeriodRepository
import com.veleda.cyclewise.testutil.TestData
import com.veleda.cyclewise.testutil.buildPeriod
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class AutoCloseOngoingPeriodUseCaseTest {

    private lateinit var mockRepository: PeriodRepository
    private lateinit var useCase: AutoCloseOngoingPeriodUseCase

    @BeforeTest
    fun setUp() {
        mockRepository = mockk(relaxed = true)
        useCase = AutoCloseOngoingPeriodUseCase(mockRepository)
    }

    @Test
    fun invoke_WHEN_noOngoingPeriodExists_THEN_doesNothing() = runTest {
        // ARRANGE
        coEvery { mockRepository.getCurrentlyOngoingPeriod() } returns null

        // ACT
        useCase()

        // ASSERT
        coVerify(exactly = 0) { mockRepository.updatePeriodEndDate(any(), any()) }
        coVerify(exactly = 0) { mockRepository.observeAllPeriodDays() }
    }

    @Test
    fun invoke_WHEN_ongoingPeriodExistsAndLastLoggedDayIsToday_THEN_doesNotClose() = runTest {
        // ARRANGE
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val ongoingPeriod = buildPeriod(
            id = "period-1",
            startDate = today.minus(3, DateTimeUnit.DAY),
            endDate = null
        )
        coEvery { mockRepository.getCurrentlyOngoingPeriod() } returns ongoingPeriod
        coEvery { mockRepository.observeAllPeriodDays() } returns flowOf(
            setOf(
                today.minus(3, DateTimeUnit.DAY),
                today.minus(2, DateTimeUnit.DAY),
                today.minus(1, DateTimeUnit.DAY),
                today
            )
        )

        // ACT
        useCase()

        // ASSERT — last logged day is today, so dateToCheck = tomorrow > today → no close
        coVerify(exactly = 0) { mockRepository.updatePeriodEndDate(any(), any()) }
    }

    @Test
    fun invoke_WHEN_ongoingPeriodExistsAndGapDetected_THEN_closesPeriodAtLastLoggedDay() = runTest {
        // ARRANGE
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val periodStart = today.minus(5, DateTimeUnit.DAY)
        val lastLoggedDay = today.minus(2, DateTimeUnit.DAY)
        val ongoingPeriod = buildPeriod(
            id = "period-1",
            startDate = periodStart,
            endDate = null
        )
        coEvery { mockRepository.getCurrentlyOngoingPeriod() } returns ongoingPeriod
        coEvery { mockRepository.observeAllPeriodDays() } returns flowOf(
            setOf(periodStart, periodStart.plus(1, DateTimeUnit.DAY), lastLoggedDay)
        )

        // ACT
        useCase()

        // ASSERT — lastLoggedDay + 1 <= today, so period should be closed at lastLoggedDay
        coVerify(exactly = 1) {
            mockRepository.updatePeriodEndDate("period-1", lastLoggedDay)
        }
    }

    @Test
    fun invoke_WHEN_ongoingPeriodExistsAndNoPeriodDaysLogged_THEN_closesAtStartDate() = runTest {
        // ARRANGE
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val periodStart = today.minus(3, DateTimeUnit.DAY)
        val ongoingPeriod = buildPeriod(
            id = "period-1",
            startDate = periodStart,
            endDate = null
        )
        coEvery { mockRepository.getCurrentlyOngoingPeriod() } returns ongoingPeriod
        coEvery { mockRepository.observeAllPeriodDays() } returns flowOf(emptySet())

        // ACT
        useCase()

        // ASSERT — no period days logged, falls back to startDate
        coVerify(exactly = 1) {
            mockRepository.updatePeriodEndDate("period-1", periodStart)
        }
    }

    @Test
    fun invoke_WHEN_ongoingPeriodExistsAndLastLoggedDayWasYesterday_THEN_closesAtYesterday() = runTest {
        // ARRANGE
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val yesterday = today.minus(1, DateTimeUnit.DAY)
        val periodStart = today.minus(4, DateTimeUnit.DAY)
        val ongoingPeriod = buildPeriod(
            id = "period-1",
            startDate = periodStart,
            endDate = null
        )
        coEvery { mockRepository.getCurrentlyOngoingPeriod() } returns ongoingPeriod
        coEvery { mockRepository.observeAllPeriodDays() } returns flowOf(
            setOf(periodStart, yesterday)
        )

        // ACT
        useCase()

        // ASSERT — yesterday + 1 = today, dateToCheck <= today → closes at yesterday
        coVerify(exactly = 1) {
            mockRepository.updatePeriodEndDate("period-1", yesterday)
        }
    }

    @Test
    fun invoke_WHEN_periodDaysFromOtherPeriodsExist_THEN_filtersToOngoingPeriodDaysOnly() = runTest {
        // ARRANGE
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val periodStart = today.minus(2, DateTimeUnit.DAY)
        val ongoingPeriod = buildPeriod(
            id = "period-2",
            startDate = periodStart,
            endDate = null
        )
        // Include period days from a past period (before ongoing period's start)
        val pastPeriodDay = today.minus(30, DateTimeUnit.DAY)
        coEvery { mockRepository.getCurrentlyOngoingPeriod() } returns ongoingPeriod
        coEvery { mockRepository.observeAllPeriodDays() } returns flowOf(
            setOf(pastPeriodDay, periodStart, today)
        )

        // ACT
        useCase()

        // ASSERT — last logged day within the ongoing period is today → no close
        coVerify(exactly = 0) { mockRepository.updatePeriodEndDate(any(), any()) }
    }
}
