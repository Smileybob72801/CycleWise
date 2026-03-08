package com.veleda.cyclewise.domain.insights.generators

import com.veleda.cyclewise.domain.insights.EnergyPhasePattern
import com.veleda.cyclewise.testutil.TestData
import com.veleda.cyclewise.testutil.buildDailyEntry
import com.veleda.cyclewise.testutil.buildFullDailyLog
import com.veleda.cyclewise.testutil.buildPeriod
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.ExperimentalTime

/**
 * Unit tests for [EnergyPhasePatternGenerator].
 *
 * Verifies detection of recurring energy-to-cycle-phase correlations,
 * boundary conditions for minimum data, and low/high threshold filtering.
 */
@OptIn(ExperimentalTime::class)
class EnergyPhasePatternGeneratorTest {

    private val generator = EnergyPhasePatternGenerator()

    @Test
    fun generate_WHEN_twoCompletedPeriodsWithThreeLowEnergyDays_THEN_producesEnergyPattern() {
        // GIVEN - 2 completed periods forming 1 cycle, 3 consecutive low-energy days (score <= 2)
        val p1Start = TestData.DATE.minus(28, DateTimeUnit.DAY)
        val p2Start = TestData.DATE

        val periods = listOf(
            buildPeriod(startDate = p2Start, endDate = p2Start.plus(4, DateTimeUnit.DAY)),
            buildPeriod(startDate = p1Start, endDate = p1Start.plus(4, DateTimeUnit.DAY))
        )

        val logs = (1..3).map { day ->
            buildFullDailyLog(
                entry = buildDailyEntry(
                    id = "entry-day$day",
                    entryDate = p1Start.plus(day - 1, DateTimeUnit.DAY),
                    dayInCycle = day,
                    energyLevel = 1
                )
            )
        }

        val data = InsightData(
            allPeriods = periods,
            allLogs = logs,
            symptomLibrary = emptyList(),
            averageCycleLength = 28.0,
            topSymptomsCount = 3
        )

        // WHEN
        val insights = generator.generate(data)

        // THEN
        assertTrue(insights.isNotEmpty(), "Should produce an energy pattern with 2 completed periods and 3 low-energy days")
        assertTrue(
            insights.all { it is EnergyPhasePattern },
            "All insights should be EnergyPhasePattern"
        )
    }

    @Test
    fun generate_WHEN_oneCompletedPeriod_THEN_returnsEmpty() {
        // GIVEN - only 1 completed period (below the 2-period threshold)
        val periods = listOf(
            buildPeriod(
                startDate = TestData.DATE,
                endDate = TestData.DATE.plus(4, DateTimeUnit.DAY)
            )
        )

        val data = InsightData(
            allPeriods = periods,
            allLogs = emptyList(),
            symptomLibrary = emptyList(),
            averageCycleLength = null,
            topSymptomsCount = 3
        )

        // WHEN
        val insights = generator.generate(data)

        // THEN
        assertTrue(insights.isEmpty(), "Should return empty with only 1 completed period")
    }

    @Test
    fun generate_WHEN_energyScoresInMidRange_THEN_returnsEmpty() {
        // GIVEN - 2 completed periods but energy scores are 3 (mid-range, not low <= 2 or high >= 4)
        val p1Start = TestData.DATE.minus(28, DateTimeUnit.DAY)
        val p2Start = TestData.DATE

        val periods = listOf(
            buildPeriod(startDate = p2Start, endDate = p2Start.plus(4, DateTimeUnit.DAY)),
            buildPeriod(startDate = p1Start, endDate = p1Start.plus(4, DateTimeUnit.DAY))
        )

        val logs = (1..5).map { day ->
            buildFullDailyLog(
                entry = buildDailyEntry(
                    id = "entry-day$day",
                    entryDate = p1Start.plus(day - 1, DateTimeUnit.DAY),
                    dayInCycle = day,
                    energyLevel = 3
                )
            )
        }

        val data = InsightData(
            allPeriods = periods,
            allLogs = logs,
            symptomLibrary = emptyList(),
            averageCycleLength = 28.0,
            topSymptomsCount = 3
        )

        // WHEN
        val insights = generator.generate(data)

        // THEN
        assertTrue(insights.isEmpty(), "Mid-range energy scores should not produce patterns")
    }

    @Test
    fun generate_WHEN_highEnergyData_THEN_producesHighEnergyPattern() {
        // GIVEN - 2 completed periods, 3 consecutive high-energy days (score >= 4)
        val p1Start = TestData.DATE.minus(28, DateTimeUnit.DAY)
        val p2Start = TestData.DATE

        val periods = listOf(
            buildPeriod(startDate = p2Start, endDate = p2Start.plus(4, DateTimeUnit.DAY)),
            buildPeriod(startDate = p1Start, endDate = p1Start.plus(4, DateTimeUnit.DAY))
        )

        val logs = (1..3).map { day ->
            buildFullDailyLog(
                entry = buildDailyEntry(
                    id = "entry-day$day",
                    entryDate = p1Start.plus(day - 1, DateTimeUnit.DAY),
                    dayInCycle = day,
                    energyLevel = 5
                )
            )
        }

        val data = InsightData(
            allPeriods = periods,
            allLogs = logs,
            symptomLibrary = emptyList(),
            averageCycleLength = 28.0,
            topSymptomsCount = 3
        )

        // WHEN
        val insights = generator.generate(data)

        // THEN
        assertTrue(insights.isNotEmpty(), "Should produce a high-energy pattern")
        val pattern = insights.first() as EnergyPhasePattern
        assertEquals("high", pattern.energyType, "Energy type should be 'high'")
    }
}
