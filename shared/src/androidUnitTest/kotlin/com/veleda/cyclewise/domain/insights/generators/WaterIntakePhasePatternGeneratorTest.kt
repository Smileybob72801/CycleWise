package com.veleda.cyclewise.domain.insights.generators

import com.veleda.cyclewise.domain.insights.WaterIntakePhasePattern
import com.veleda.cyclewise.testutil.TestData
import com.veleda.cyclewise.testutil.buildDailyEntry
import com.veleda.cyclewise.testutil.buildFullDailyLog
import com.veleda.cyclewise.testutil.buildPeriod
import com.veleda.cyclewise.testutil.buildWaterIntake
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.ExperimentalTime

/**
 * Unit tests for [WaterIntakePhasePatternGenerator].
 *
 * Verifies detection of recurring water intake patterns across cycle phases,
 * minimum data guards, and low/high intake threshold filtering.
 */
@OptIn(ExperimentalTime::class)
class WaterIntakePhasePatternGeneratorTest {

    private val generator = WaterIntakePhasePatternGenerator()

    @Test
    fun generate_WHEN_twoCompletedPeriodsWithLowWaterIntake_THEN_producesWaterPattern() {
        // GIVEN - 2 completed periods, 3 consecutive days with low water intake (< 4 cups)
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
                    dayInCycle = day
                )
            )
        }

        val waterIntakes = (1..3).map { day ->
            buildWaterIntake(
                date = p1Start.plus(day - 1, DateTimeUnit.DAY),
                cups = 2 // Below LOW_THRESHOLD of 4
            )
        }

        val data = InsightData(
            allPeriods = periods,
            allLogs = logs,
            symptomLibrary = emptyList(),
            averageCycleLength = 28.0,
            topSymptomsCount = 3,
            waterIntakes = waterIntakes
        )

        // WHEN
        val insights = generator.generate(data)

        // THEN
        assertTrue(insights.isNotEmpty(), "Should produce a water intake pattern with low intake data")
        assertTrue(
            insights.all { it is WaterIntakePhasePattern },
            "All insights should be WaterIntakePhasePattern"
        )
        val pattern = insights.first() as WaterIntakePhasePattern
        assertEquals("low", pattern.intakeLevel, "Intake level should be 'low'")
    }

    @Test
    fun generate_WHEN_noWaterIntakeData_THEN_returnsEmpty() {
        // GIVEN - 2 completed periods but no water intake data
        val p1Start = TestData.DATE.minus(28, DateTimeUnit.DAY)
        val p2Start = TestData.DATE

        val periods = listOf(
            buildPeriod(startDate = p2Start, endDate = p2Start.plus(4, DateTimeUnit.DAY)),
            buildPeriod(startDate = p1Start, endDate = p1Start.plus(4, DateTimeUnit.DAY))
        )

        val data = InsightData(
            allPeriods = periods,
            allLogs = emptyList(),
            symptomLibrary = emptyList(),
            averageCycleLength = 28.0,
            topSymptomsCount = 3,
            waterIntakes = emptyList()
        )

        // WHEN
        val insights = generator.generate(data)

        // THEN
        assertTrue(insights.isEmpty(), "Should return empty when no water intake data exists")
    }

    @Test
    fun generate_WHEN_waterIntakeInMidRange_THEN_returnsEmpty() {
        // GIVEN - 2 completed periods, water intake is 5 cups (>= 4 but < 6, neither low nor high)
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
                    dayInCycle = day
                )
            )
        }

        val waterIntakes = (1..5).map { day ->
            buildWaterIntake(
                date = p1Start.plus(day - 1, DateTimeUnit.DAY),
                cups = 5 // Mid-range: >= 4 (not low) and < 6 (not high)
            )
        }

        val data = InsightData(
            allPeriods = periods,
            allLogs = logs,
            symptomLibrary = emptyList(),
            averageCycleLength = 28.0,
            topSymptomsCount = 3,
            waterIntakes = waterIntakes
        )

        // WHEN
        val insights = generator.generate(data)

        // THEN
        assertTrue(insights.isEmpty(), "Mid-range water intake should not produce patterns")
    }

    @Test
    fun generate_WHEN_oneCompletedPeriod_THEN_returnsEmpty() {
        // GIVEN - only 1 completed period
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
            topSymptomsCount = 3,
            waterIntakes = listOf(buildWaterIntake(cups = 2))
        )

        // WHEN
        val insights = generator.generate(data)

        // THEN
        assertTrue(insights.isEmpty(), "Should return empty with only 1 completed period")
    }
}
