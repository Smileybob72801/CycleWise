package com.veleda.cyclewise.domain.insights.generators

import com.veleda.cyclewise.domain.insights.FlowPattern
import com.veleda.cyclewise.domain.models.FlowIntensity
import com.veleda.cyclewise.testutil.TestData
import com.veleda.cyclewise.testutil.buildDailyEntry
import com.veleda.cyclewise.testutil.buildFullDailyLog
import com.veleda.cyclewise.testutil.buildPeriod
import com.veleda.cyclewise.testutil.buildPeriodLog
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.time.ExperimentalTime

/**
 * Unit tests for [FlowPatternGenerator].
 *
 * Verifies detection of recurring flow intensity patterns across cycles,
 * minimum data guards, and the relaxed block parameters (maxGap=1, minBlockSize=1).
 */
@OptIn(ExperimentalTime::class)
class FlowPatternGeneratorTest {

    private val generator = FlowPatternGenerator()

    @Test
    fun generate_WHEN_twoCompletedPeriodsWithFlowData_THEN_producesFlowPattern() {
        // GIVEN - 2 completed periods with heavy flow logged on the same cycle day
        val p1Start = TestData.DATE.minus(28, DateTimeUnit.DAY)
        val p2Start = TestData.DATE

        val periods = listOf(
            buildPeriod(startDate = p2Start, endDate = p2Start.plus(4, DateTimeUnit.DAY)),
            buildPeriod(startDate = p1Start, endDate = p1Start.plus(4, DateTimeUnit.DAY))
        )

        // Flow logs for cycle 1 (days 1-3 heavy)
        val logsC1 = (1..3).map { day ->
            val entryId = "entry-c1-day$day"
            buildFullDailyLog(
                entry = buildDailyEntry(
                    id = entryId,
                    entryDate = p1Start.plus(day - 1, DateTimeUnit.DAY),
                    dayInCycle = day
                ),
                periodLog = buildPeriodLog(
                    entryId = entryId,
                    flowIntensity = FlowIntensity.HEAVY
                )
            )
        }

        val data = InsightData(
            allPeriods = periods,
            allLogs = logsC1,
            symptomLibrary = emptyList(),
            averageCycleLength = 28.0,
            topSymptomsCount = 3
        )

        // WHEN
        val insights = generator.generate(data)

        // THEN
        assertTrue(insights.isNotEmpty(), "Should produce flow patterns with 2 completed periods and flow data")
        assertTrue(
            insights.all { it is FlowPattern },
            "All insights should be FlowPattern"
        )
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
            topSymptomsCount = 3
        )

        // WHEN
        val insights = generator.generate(data)

        // THEN
        assertTrue(insights.isEmpty(), "Should return empty with only 1 completed period")
    }

    @Test
    fun generate_WHEN_noFlowDataInLogs_THEN_returnsEmpty() {
        // GIVEN - 2 completed periods but no period logs with flow intensity
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
                // No periodLog -> no flow intensity
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
        assertTrue(insights.isEmpty(), "Should return empty when no flow intensity data exists")
    }
}
