package com.veleda.cyclewise.domain.insights.generators

import com.veleda.cyclewise.domain.insights.SymptomSeverityTrend
import com.veleda.cyclewise.testutil.TestData
import com.veleda.cyclewise.testutil.buildDailyEntry
import com.veleda.cyclewise.testutil.buildFullDailyLog
import com.veleda.cyclewise.testutil.buildPeriod
import com.veleda.cyclewise.testutil.buildSymptomLog
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.ExperimentalTime

/**
 * Unit tests for [SymptomSeverityTrendGenerator].
 *
 * Verifies trend detection across older vs. recent cycles, minimum data guards,
 * and the significance threshold of 0.3 for trend classification.
 */
@OptIn(ExperimentalTime::class)
class SymptomSeverityTrendGeneratorTest {

    private val generator = SymptomSeverityTrendGenerator()

    @Test
    fun generate_WHEN_increasingSeverityAcrossCycles_THEN_producesIncreasingTrend() {
        // GIVEN - 3 completed periods (2 full cycles), older cycle has low severity, recent has high
        val p1Start = TestData.DATE.minus(56, DateTimeUnit.DAY)
        val p2Start = TestData.DATE.minus(28, DateTimeUnit.DAY)
        val p3Start = TestData.DATE

        val periods = listOf(
            buildPeriod(startDate = p3Start, endDate = p3Start.plus(4, DateTimeUnit.DAY)),
            buildPeriod(startDate = p2Start, endDate = p2Start.plus(4, DateTimeUnit.DAY)),
            buildPeriod(startDate = p1Start, endDate = p1Start.plus(4, DateTimeUnit.DAY))
        )

        // Older cycle (p1 -> p2): low severity = 1
        val olderLogs = (1..3).map { day ->
            val entryId = "entry-older-$day"
            buildFullDailyLog(
                entry = buildDailyEntry(
                    id = entryId,
                    entryDate = p1Start.plus(day - 1, DateTimeUnit.DAY),
                    dayInCycle = day
                ),
                symptomLogs = listOf(buildSymptomLog(entryId = entryId, severity = 1))
            )
        }

        // Recent cycle (p2 -> p3): high severity = 4
        val recentLogs = (1..3).map { day ->
            val entryId = "entry-recent-$day"
            buildFullDailyLog(
                entry = buildDailyEntry(
                    id = entryId,
                    entryDate = p2Start.plus(day - 1, DateTimeUnit.DAY),
                    dayInCycle = day
                ),
                symptomLogs = listOf(buildSymptomLog(entryId = entryId, severity = 4))
            )
        }

        val data = InsightData(
            allPeriods = periods,
            allLogs = olderLogs + recentLogs,
            symptomLibrary = emptyList(),
            averageCycleLength = 28.0,
            topSymptomsCount = 3
        )

        // WHEN
        val insights = generator.generate(data)

        // THEN
        assertEquals(1, insights.size, "Should produce exactly 1 severity trend insight")
        val trend = insights.first() as SymptomSeverityTrend
        assertEquals("increasing", trend.trendDescription, "Trend should be 'increasing'")
        assertTrue(trend.avgSeverityRecent > trend.avgSeverityOlder, "Recent severity should be higher")
    }

    @Test
    fun generate_WHEN_stableSeverity_THEN_producesStableTrend() {
        // GIVEN - 3 completed periods with similar severity across all cycles
        val p1Start = TestData.DATE.minus(56, DateTimeUnit.DAY)
        val p2Start = TestData.DATE.minus(28, DateTimeUnit.DAY)
        val p3Start = TestData.DATE

        val periods = listOf(
            buildPeriod(startDate = p3Start, endDate = p3Start.plus(4, DateTimeUnit.DAY)),
            buildPeriod(startDate = p2Start, endDate = p2Start.plus(4, DateTimeUnit.DAY)),
            buildPeriod(startDate = p1Start, endDate = p1Start.plus(4, DateTimeUnit.DAY))
        )

        // Both cycles have same severity = 3
        val olderLogs = (1..3).map { day ->
            val entryId = "entry-older-$day"
            buildFullDailyLog(
                entry = buildDailyEntry(
                    id = entryId,
                    entryDate = p1Start.plus(day - 1, DateTimeUnit.DAY),
                    dayInCycle = day
                ),
                symptomLogs = listOf(buildSymptomLog(entryId = entryId, severity = 3))
            )
        }

        val recentLogs = (1..3).map { day ->
            val entryId = "entry-recent-$day"
            buildFullDailyLog(
                entry = buildDailyEntry(
                    id = entryId,
                    entryDate = p2Start.plus(day - 1, DateTimeUnit.DAY),
                    dayInCycle = day
                ),
                symptomLogs = listOf(buildSymptomLog(entryId = entryId, severity = 3))
            )
        }

        val data = InsightData(
            allPeriods = periods,
            allLogs = olderLogs + recentLogs,
            symptomLibrary = emptyList(),
            averageCycleLength = 28.0,
            topSymptomsCount = 3
        )

        // WHEN
        val insights = generator.generate(data)

        // THEN
        assertEquals(1, insights.size, "Should produce exactly 1 severity trend insight")
        val trend = insights.first() as SymptomSeverityTrend
        assertEquals("stable", trend.trendDescription, "Trend should be 'stable' when difference <= 0.3")
    }

    @Test
    fun generate_WHEN_fewerThanTwoCycleBoundaries_THEN_returnsEmpty() {
        // GIVEN - only 2 completed periods but boundaries.size < 2 means < 3 periods total
        val periods = listOf(
            buildPeriod(
                startDate = TestData.DATE,
                endDate = TestData.DATE.plus(4, DateTimeUnit.DAY)
            ),
            buildPeriod(
                startDate = TestData.DATE.minus(28, DateTimeUnit.DAY),
                endDate = TestData.DATE.minus(24, DateTimeUnit.DAY)
            )
        )

        val data = InsightData(
            allPeriods = periods,
            allLogs = emptyList(),
            symptomLibrary = emptyList(),
            averageCycleLength = 28.0,
            topSymptomsCount = 3
        )

        // WHEN
        val insights = generator.generate(data)

        // THEN — boundaries.size < 2 requires at least 3 completed periods to get 2 boundaries,
        // or if we do get boundaries but no symptom data, it returns empty due to nonEmptyCycles < 2
        assertTrue(insights.isEmpty(), "Should return empty without sufficient symptom data across cycles")
    }

    @Test
    fun generate_WHEN_noSymptomLogs_THEN_returnsEmpty() {
        // GIVEN - 3 completed periods but no symptom logs
        val p1Start = TestData.DATE.minus(56, DateTimeUnit.DAY)
        val p2Start = TestData.DATE.minus(28, DateTimeUnit.DAY)
        val p3Start = TestData.DATE

        val periods = listOf(
            buildPeriod(startDate = p3Start, endDate = p3Start.plus(4, DateTimeUnit.DAY)),
            buildPeriod(startDate = p2Start, endDate = p2Start.plus(4, DateTimeUnit.DAY)),
            buildPeriod(startDate = p1Start, endDate = p1Start.plus(4, DateTimeUnit.DAY))
        )

        val data = InsightData(
            allPeriods = periods,
            allLogs = emptyList(),
            symptomLibrary = emptyList(),
            averageCycleLength = 28.0,
            topSymptomsCount = 3
        )

        // WHEN
        val insights = generator.generate(data)

        // THEN
        assertTrue(insights.isEmpty(), "Should return empty when no symptom logs exist")
    }
}
