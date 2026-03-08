package com.veleda.cyclewise.domain.insights.generators

import com.veleda.cyclewise.domain.insights.LibidoPhasePattern
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
 * Unit tests for [LibidoPhasePatternGenerator].
 *
 * Verifies detection of recurring libido-to-cycle-phase correlations,
 * minimum data guards, and low/high threshold filtering.
 */
@OptIn(ExperimentalTime::class)
class LibidoPhasePatternGeneratorTest {

    private val generator = LibidoPhasePatternGenerator()

    @Test
    fun generate_WHEN_twoCompletedPeriodsWithThreeLowLibidoDays_THEN_producesLibidoPattern() {
        // GIVEN - 2 completed periods, 3 consecutive low-libido days (score <= 2)
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
                    libidoScore = 1
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
        assertTrue(insights.isNotEmpty(), "Should produce a libido pattern with 2 completed periods and 3 low-libido days")
        assertTrue(
            insights.all { it is LibidoPhasePattern },
            "All insights should be LibidoPhasePattern"
        )
    }

    @Test
    fun generate_WHEN_oneCompletedPeriod_THEN_returnsEmpty() {
        // GIVEN - only 1 completed period (below threshold)
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
    fun generate_WHEN_highLibidoData_THEN_producesHighLibidoPattern() {
        // GIVEN - 2 completed periods, 3 consecutive high-libido days (score >= 4)
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
                    libidoScore = 5
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
        assertTrue(insights.isNotEmpty(), "Should produce a high-libido pattern")
        val pattern = insights.first() as LibidoPhasePattern
        assertEquals("high", pattern.libidoType, "Libido type should be 'high'")
    }

    @Test
    fun generate_WHEN_libidoScoresInMidRange_THEN_returnsEmpty() {
        // GIVEN - 2 completed periods but libido scores are 3 (mid-range)
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
                    libidoScore = 3
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
        assertTrue(insights.isEmpty(), "Mid-range libido scores should not produce patterns")
    }
}
