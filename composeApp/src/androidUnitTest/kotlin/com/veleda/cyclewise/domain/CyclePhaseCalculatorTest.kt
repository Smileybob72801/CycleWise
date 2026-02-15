package com.veleda.cyclewise.domain

import com.veleda.cyclewise.domain.models.CyclePhase
import com.veleda.cyclewise.testutil.TestData
import com.veleda.cyclewise.testutil.buildPeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.plus
import kotlinx.datetime.minus
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CyclePhaseCalculatorTest {

    // ==================== calculatePhase tests ====================

    @Test
    fun calculatePhase_WHEN_noPeriods_THEN_returnsNull() {
        // ARRANGE
        val date = TestData.DATE

        // ACT
        val result = CyclePhaseCalculator.calculatePhase(date, emptyList(), 28.0)

        // ASSERT
        assertNull(result)
    }

    @Test
    fun calculatePhase_WHEN_dateBeforeFirstPeriod_THEN_returnsNull() {
        // ARRANGE
        val periodStart = LocalDate(2025, 3, 1)
        val periods = listOf(buildPeriod(startDate = periodStart, endDate = LocalDate(2025, 3, 5)))
        val dateBefore = LocalDate(2025, 2, 20)

        // ACT
        val result = CyclePhaseCalculator.calculatePhase(dateBefore, periods, 28.0)

        // ASSERT
        assertNull(result)
    }

    @Test
    fun calculatePhase_WHEN_dateDuringPeriod_THEN_returnsMenstruation() {
        // ARRANGE
        val period1Start = LocalDate(2025, 3, 1)
        val period1End = LocalDate(2025, 3, 5)
        val period2Start = LocalDate(2025, 3, 29)
        val period2End = LocalDate(2025, 4, 2)
        val periods = listOf(
            buildPeriod(startDate = period1Start, endDate = period1End),
            buildPeriod(startDate = period2Start, endDate = period2End)
        )
        val dateInPeriod = LocalDate(2025, 3, 3) // Day 3 of first period

        // ACT
        val result = CyclePhaseCalculator.calculatePhase(dateInPeriod, periods, 28.0)

        // ASSERT
        assertEquals(CyclePhase.MENSTRUATION, result)
    }

    @Test
    fun calculatePhase_WHEN_dateOnPeriodStartDay_THEN_returnsMenstruation() {
        // ARRANGE
        val periodStart = LocalDate(2025, 3, 1)
        val periods = listOf(
            buildPeriod(startDate = periodStart, endDate = LocalDate(2025, 3, 5)),
            buildPeriod(startDate = LocalDate(2025, 3, 29), endDate = LocalDate(2025, 4, 2))
        )

        // ACT
        val result = CyclePhaseCalculator.calculatePhase(periodStart, periods, 28.0)

        // ASSERT
        assertEquals(CyclePhase.MENSTRUATION, result)
    }

    @Test
    fun calculatePhase_WHEN_dateOnPeriodEndDay_THEN_returnsMenstruation() {
        // ARRANGE
        val periodEnd = LocalDate(2025, 3, 5)
        val periods = listOf(
            buildPeriod(startDate = LocalDate(2025, 3, 1), endDate = periodEnd),
            buildPeriod(startDate = LocalDate(2025, 3, 29), endDate = LocalDate(2025, 4, 2))
        )

        // ACT
        val result = CyclePhaseCalculator.calculatePhase(periodEnd, periods, 28.0)

        // ASSERT
        assertEquals(CyclePhase.MENSTRUATION, result)
    }

    @Test
    fun calculatePhase_WHEN_dateInFollicularPhase_THEN_returnsFollicular() {
        // ARRANGE — 28-day cycle: period Mar 1-5, next period Mar 29
        // Luteal starts day 15 (Mar 15), ovulation days 12-14 (Mar 12-14)
        // Follicular: days 6-11 (Mar 6-11)
        val periods = listOf(
            buildPeriod(startDate = LocalDate(2025, 3, 1), endDate = LocalDate(2025, 3, 5)),
            buildPeriod(startDate = LocalDate(2025, 3, 29), endDate = LocalDate(2025, 4, 2))
        )
        val follicularDate = LocalDate(2025, 3, 8) // Day 8 of cycle

        // ACT
        val result = CyclePhaseCalculator.calculatePhase(follicularDate, periods, 28.0)

        // ASSERT
        assertEquals(CyclePhase.FOLLICULAR, result)
    }

    @Test
    fun calculatePhase_WHEN_dateInOvulationWindow_THEN_returnsOvulation() {
        // ARRANGE — 28-day cycle: period Mar 1-5, next period Mar 29
        // Cycle length = 28, luteal starts at day 15, ovulation = days 12-14
        val periods = listOf(
            buildPeriod(startDate = LocalDate(2025, 3, 1), endDate = LocalDate(2025, 3, 5)),
            buildPeriod(startDate = LocalDate(2025, 3, 29), endDate = LocalDate(2025, 4, 2))
        )
        val ovulationDate = LocalDate(2025, 3, 13) // Day 13 of 28-day cycle

        // ACT
        val result = CyclePhaseCalculator.calculatePhase(ovulationDate, periods, 28.0)

        // ASSERT
        assertEquals(CyclePhase.OVULATION, result)
    }

    @Test
    fun calculatePhase_WHEN_dateInLutealPhase_THEN_returnsLuteal() {
        // ARRANGE — 28-day cycle: period Mar 1-5, next period Mar 29
        // Luteal phase: days 15-28 (Mar 15-28)
        val periods = listOf(
            buildPeriod(startDate = LocalDate(2025, 3, 1), endDate = LocalDate(2025, 3, 5)),
            buildPeriod(startDate = LocalDate(2025, 3, 29), endDate = LocalDate(2025, 4, 2))
        )
        val lutealDate = LocalDate(2025, 3, 20) // Day 20 of 28-day cycle

        // ACT
        val result = CyclePhaseCalculator.calculatePhase(lutealDate, periods, 28.0)

        // ASSERT
        assertEquals(CyclePhase.LUTEAL, result)
    }

    @Test
    fun calculatePhase_WHEN_standard28DayCycle_THEN_allPhasesAtExpectedBoundaries() {
        // ARRANGE — Classic 28-day cycle: period days 1-5
        // Follicular: days 6-11, Ovulation: days 12-14, Luteal: days 15-28
        val cycleStart = LocalDate(2025, 3, 1)
        val cycleEnd = LocalDate(2025, 3, 5)
        val nextCycleStart = LocalDate(2025, 3, 29) // 28 days later
        val periods = listOf(
            buildPeriod(startDate = cycleStart, endDate = cycleEnd),
            buildPeriod(startDate = nextCycleStart, endDate = LocalDate(2025, 4, 2))
        )

        // ACT & ASSERT — Check each phase boundary
        // Day 1 (Mar 1) = MENSTRUATION
        assertEquals(CyclePhase.MENSTRUATION, CyclePhaseCalculator.calculatePhase(LocalDate(2025, 3, 1), periods, 28.0))
        // Day 5 (Mar 5) = MENSTRUATION (last period day)
        assertEquals(CyclePhase.MENSTRUATION, CyclePhaseCalculator.calculatePhase(LocalDate(2025, 3, 5), periods, 28.0))
        // Day 6 (Mar 6) = FOLLICULAR (first post-period day)
        assertEquals(CyclePhase.FOLLICULAR, CyclePhaseCalculator.calculatePhase(LocalDate(2025, 3, 6), periods, 28.0))
        // Day 11 (Mar 11) = FOLLICULAR (last follicular day)
        assertEquals(CyclePhase.FOLLICULAR, CyclePhaseCalculator.calculatePhase(LocalDate(2025, 3, 11), periods, 28.0))
        // Day 12 (Mar 12) = OVULATION (first ovulation day)
        assertEquals(CyclePhase.OVULATION, CyclePhaseCalculator.calculatePhase(LocalDate(2025, 3, 12), periods, 28.0))
        // Day 14 (Mar 14) = OVULATION (last ovulation day)
        assertEquals(CyclePhase.OVULATION, CyclePhaseCalculator.calculatePhase(LocalDate(2025, 3, 14), periods, 28.0))
        // Day 15 (Mar 15) = LUTEAL (first luteal day)
        assertEquals(CyclePhase.LUTEAL, CyclePhaseCalculator.calculatePhase(LocalDate(2025, 3, 15), periods, 28.0))
        // Day 28 (Mar 28) = LUTEAL (last day of cycle)
        assertEquals(CyclePhase.LUTEAL, CyclePhaseCalculator.calculatePhase(LocalDate(2025, 3, 28), periods, 28.0))
    }

    @Test
    fun calculatePhase_WHEN_ongoingCycleWithAverage_THEN_returnsCorrectPhase() {
        // ARRANGE — One ongoing period (no end date implied through completed periods for avg),
        // plus a completed period to establish an average
        // We need at least 2 completed periods for averageCycleLength, but calculatePhase
        // accepts average as a parameter directly
        val periods = listOf(
            buildPeriod(startDate = LocalDate(2025, 3, 1), endDate = LocalDate(2025, 3, 5))
        )
        // Date in the follicular phase of the latest cycle, using avg of 28
        val follicularDate = LocalDate(2025, 3, 8) // Day 8

        // ACT
        val result = CyclePhaseCalculator.calculatePhase(follicularDate, periods, 28.0)

        // ASSERT
        assertEquals(CyclePhase.FOLLICULAR, result)
    }

    @Test
    fun calculatePhase_WHEN_ongoingCycleWithAverage_THEN_lutealPredicted() {
        // ARRANGE — Single completed period, date falls in predicted luteal zone
        val periods = listOf(
            buildPeriod(startDate = LocalDate(2025, 3, 1), endDate = LocalDate(2025, 3, 5))
        )
        val lutealDate = LocalDate(2025, 3, 20) // Day 20 of predicted 28-day cycle

        // ACT
        val result = CyclePhaseCalculator.calculatePhase(lutealDate, periods, 28.0)

        // ASSERT
        assertEquals(CyclePhase.LUTEAL, result)
    }

    @Test
    fun calculatePhase_WHEN_nullAverageAndPastLastPeriod_THEN_returnsNull() {
        // ARRANGE — Single completed period, no average available, date after period ends
        val periods = listOf(
            buildPeriod(startDate = LocalDate(2025, 3, 1), endDate = LocalDate(2025, 3, 5))
        )
        val dateAfter = LocalDate(2025, 3, 10)

        // ACT
        val result = CyclePhaseCalculator.calculatePhase(dateAfter, periods, null)

        // ASSERT
        assertNull(result)
    }

    @Test
    fun calculatePhase_WHEN_shortCycle21Days_THEN_phasesStillValid() {
        // ARRANGE — 21-day cycle: period Mar 1-4, next period Mar 22
        // Luteal starts day 8 (21-14+1), ovulation days 5-7, follicular has no room
        // (period ends day 4, ovulation starts day 5)
        val periods = listOf(
            buildPeriod(startDate = LocalDate(2025, 3, 1), endDate = LocalDate(2025, 3, 4)),
            buildPeriod(startDate = LocalDate(2025, 3, 22), endDate = LocalDate(2025, 3, 25))
        )

        // Day 4 (Mar 4) = MENSTRUATION
        assertEquals(CyclePhase.MENSTRUATION, CyclePhaseCalculator.calculatePhase(LocalDate(2025, 3, 4), periods, null))
        // Day 5 (Mar 5) = OVULATION (ovulation window starts immediately after period)
        assertEquals(CyclePhase.OVULATION, CyclePhaseCalculator.calculatePhase(LocalDate(2025, 3, 5), periods, null))
        // Day 7 (Mar 7) = OVULATION
        assertEquals(CyclePhase.OVULATION, CyclePhaseCalculator.calculatePhase(LocalDate(2025, 3, 7), periods, null))
        // Day 8 (Mar 8) = LUTEAL
        assertEquals(CyclePhase.LUTEAL, CyclePhaseCalculator.calculatePhase(LocalDate(2025, 3, 8), periods, null))
        // Day 21 (Mar 21) = LUTEAL
        assertEquals(CyclePhase.LUTEAL, CyclePhaseCalculator.calculatePhase(LocalDate(2025, 3, 21), periods, null))
    }

    @Test
    fun calculatePhase_WHEN_datePastExpectedCycleEnd_THEN_returnsNull() {
        // ARRANGE — Single completed period, avg = 28, date is day 30
        val periods = listOf(
            buildPeriod(startDate = LocalDate(2025, 3, 1), endDate = LocalDate(2025, 3, 5))
        )
        val dateWayAfter = LocalDate(2025, 3, 31) // Day 31, past the 28-day cycle

        // ACT
        val result = CyclePhaseCalculator.calculatePhase(dateWayAfter, periods, 28.0)

        // ASSERT
        assertNull(result)
    }

    @Test
    fun calculatePhase_WHEN_ongoingPeriodNoEndDate_THEN_returnsMenstruation() {
        // ARRANGE — Single ongoing period with no end date
        val periods = listOf(
            buildPeriod(startDate = LocalDate(2025, 3, 1), endDate = null)
        )
        val dateInOngoing = LocalDate(2025, 3, 3)

        // ACT
        val result = CyclePhaseCalculator.calculatePhase(dateInOngoing, periods, 28.0)

        // ASSERT
        assertEquals(CyclePhase.MENSTRUATION, result)
    }

    @Test
    fun calculatePhase_WHEN_periodsAreUnsorted_THEN_sortsInternallyAndReturnsCorrectPhase() {
        // ARRANGE — Periods given in reverse order
        val periods = listOf(
            buildPeriod(startDate = LocalDate(2025, 3, 29), endDate = LocalDate(2025, 4, 2)),
            buildPeriod(startDate = LocalDate(2025, 3, 1), endDate = LocalDate(2025, 3, 5))
        )
        val follicularDate = LocalDate(2025, 3, 8) // Day 8 of first cycle

        // ACT
        val result = CyclePhaseCalculator.calculatePhase(follicularDate, periods, 28.0)

        // ASSERT
        assertEquals(CyclePhase.FOLLICULAR, result)
    }

    @Test
    fun calculatePhase_WHEN_dayAfterPeriodEnds_THEN_returnsFollicular() {
        // ARRANGE
        val periods = listOf(
            buildPeriod(startDate = LocalDate(2025, 3, 1), endDate = LocalDate(2025, 3, 5)),
            buildPeriod(startDate = LocalDate(2025, 3, 29), endDate = LocalDate(2025, 4, 2))
        )
        val dayAfterPeriod = LocalDate(2025, 3, 6) // Day 6, first post-period day

        // ACT
        val result = CyclePhaseCalculator.calculatePhase(dayAfterPeriod, periods, 28.0)

        // ASSERT
        assertEquals(CyclePhase.FOLLICULAR, result)
    }

    @Test
    fun calculatePhase_WHEN_unloggedDaysNearEndOfCompletedCycle_THEN_returnsLuteal() {
        // ARRANGE — 28-day cycle: period Mar 1-5, next period Mar 29.
        // Days 25-28 (Mar 25-28) are late luteal. Even with no log entries
        // for these days, the calculator should return LUTEAL.
        // This documents that the gap bug is in the repository layer
        // (not calling the calculator), not in the calculator itself.
        val periods = listOf(
            buildPeriod(startDate = LocalDate(2025, 3, 1), endDate = LocalDate(2025, 3, 5)),
            buildPeriod(startDate = LocalDate(2025, 3, 29), endDate = LocalDate(2025, 4, 2))
        )

        // ACT & ASSERT — days 25-28 should all be LUTEAL
        assertEquals(CyclePhase.LUTEAL, CyclePhaseCalculator.calculatePhase(LocalDate(2025, 3, 25), periods, 28.0))
        assertEquals(CyclePhase.LUTEAL, CyclePhaseCalculator.calculatePhase(LocalDate(2025, 3, 26), periods, 28.0))
        assertEquals(CyclePhase.LUTEAL, CyclePhaseCalculator.calculatePhase(LocalDate(2025, 3, 27), periods, 28.0))
        assertEquals(CyclePhase.LUTEAL, CyclePhaseCalculator.calculatePhase(LocalDate(2025, 3, 28), periods, 28.0))
    }

    // ==================== averageCycleLength tests ====================

    @Test
    fun averageCycleLength_WHEN_multipleCompletedPeriods_THEN_returnsCorrectAverage() {
        // ARRANGE — 3 completed periods with known start dates
        // Cycle 1: Mar 1 -> Mar 29 = 28 days
        // Cycle 2: Mar 29 -> Apr 28 = 30 days
        // Average = (28 + 30) / 2 = 29.0
        val periods = listOf(
            buildPeriod(startDate = LocalDate(2025, 3, 1), endDate = LocalDate(2025, 3, 5)),
            buildPeriod(startDate = LocalDate(2025, 3, 29), endDate = LocalDate(2025, 4, 2)),
            buildPeriod(startDate = LocalDate(2025, 4, 28), endDate = LocalDate(2025, 5, 2))
        )

        // ACT
        val result = CyclePhaseCalculator.averageCycleLength(periods)

        // ASSERT
        assertEquals(29.0, result)
    }

    @Test
    fun averageCycleLength_WHEN_singlePeriod_THEN_returnsNull() {
        // ARRANGE
        val periods = listOf(
            buildPeriod(startDate = LocalDate(2025, 3, 1), endDate = LocalDate(2025, 3, 5))
        )

        // ACT
        val result = CyclePhaseCalculator.averageCycleLength(periods)

        // ASSERT
        assertNull(result)
    }

    @Test
    fun averageCycleLength_WHEN_noPeriods_THEN_returnsNull() {
        // ACT
        val result = CyclePhaseCalculator.averageCycleLength(emptyList())

        // ASSERT
        assertNull(result)
    }

    @Test
    fun averageCycleLength_WHEN_ongoingPeriodExcluded_THEN_usesOnlyCompleted() {
        // ARRANGE — 2 completed + 1 ongoing; only completed should be used
        // Cycle 1: Mar 1 -> Mar 29 = 28 days
        // Ongoing period (endDate = null) should be excluded
        val periods = listOf(
            buildPeriod(startDate = LocalDate(2025, 3, 1), endDate = LocalDate(2025, 3, 5)),
            buildPeriod(startDate = LocalDate(2025, 3, 29), endDate = LocalDate(2025, 4, 2)),
            buildPeriod(startDate = LocalDate(2025, 4, 26), endDate = null)
        )

        // ACT
        val result = CyclePhaseCalculator.averageCycleLength(periods)

        // ASSERT — Only 2 completed periods, cycle length = 28
        assertEquals(28.0, result)
    }

    @Test
    fun averageCycleLength_WHEN_allOngoing_THEN_returnsNull() {
        // ARRANGE — Only ongoing periods (no end dates)
        val periods = listOf(
            buildPeriod(startDate = LocalDate(2025, 3, 1), endDate = null),
            buildPeriod(startDate = LocalDate(2025, 4, 1), endDate = null)
        )

        // ACT
        val result = CyclePhaseCalculator.averageCycleLength(periods)

        // ASSERT
        assertNull(result)
    }

    @Test
    fun averageCycleLength_WHEN_periodsUnsorted_THEN_sortsInternallyAndReturnsCorrectAverage() {
        // ARRANGE — Periods given in reverse order
        val periods = listOf(
            buildPeriod(startDate = LocalDate(2025, 4, 28), endDate = LocalDate(2025, 5, 2)),
            buildPeriod(startDate = LocalDate(2025, 3, 1), endDate = LocalDate(2025, 3, 5)),
            buildPeriod(startDate = LocalDate(2025, 3, 29), endDate = LocalDate(2025, 4, 2))
        )

        // ACT
        val result = CyclePhaseCalculator.averageCycleLength(periods)

        // ASSERT — (28 + 30) / 2 = 29.0
        assertEquals(29.0, result)
    }

    @Test
    fun averageCycleLength_WHEN_exactlyTwoCompletedPeriods_THEN_returnsDistance() {
        // ARRANGE
        val periods = listOf(
            buildPeriod(startDate = LocalDate(2025, 3, 1), endDate = LocalDate(2025, 3, 5)),
            buildPeriod(startDate = LocalDate(2025, 3, 27), endDate = LocalDate(2025, 3, 30))
        )

        // ACT
        val result = CyclePhaseCalculator.averageCycleLength(periods)

        // ASSERT — 26 days between Mar 1 and Mar 27
        assertEquals(26.0, result)
    }
}
