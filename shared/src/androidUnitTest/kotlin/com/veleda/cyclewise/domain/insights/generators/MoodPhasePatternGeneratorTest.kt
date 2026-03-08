package com.veleda.cyclewise.domain.insights.generators

import com.veleda.cyclewise.domain.insights.MoodPhasePattern
import com.veleda.cyclewise.testutil.TestData
import com.veleda.cyclewise.testutil.buildDailyEntry
import com.veleda.cyclewise.testutil.buildFullDailyLog
import com.veleda.cyclewise.testutil.buildPeriod
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.time.ExperimentalTime

/**
 * Boundary tests for the lowered activation threshold of [MoodPhasePatternGenerator].
 *
 * The generator now requires a minimum of 2 completed periods (1 full cycle) and a
 * significance count of >= 1 (down from >= 3) to detect mood patterns. Block detection
 * still requires >= 3 individually significant days within proximity gap <= 2.
 */
@OptIn(ExperimentalTime::class)
class MoodPhasePatternGeneratorTest {

    private val generator = MoodPhasePatternGenerator()

    @Test
    fun generate_WHEN_exactlyTwoCompletedPeriodsWithSufficientMoodData_THEN_producesMoodPattern() {
        // ARRANGE - 2 completed periods, 1 cycle pair, 3 consecutive low-mood days
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
                    moodScore = 1
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

        // ACT
        val insights = generator.generate(data)

        // ASSERT
        assertTrue(insights.isNotEmpty(), "Should produce a mood pattern with 2 completed periods and 3 low-mood days")
        assertTrue(
            insights.all { it is MoodPhasePattern },
            "All insights should be MoodPhasePattern"
        )
    }

    @Test
    fun generate_WHEN_oneCompletedPeriod_THEN_returnsEmpty() {
        // ARRANGE - only 1 completed period (below threshold of 2)
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

        // ACT
        val insights = generator.generate(data)

        // ASSERT
        assertTrue(insights.isEmpty(), "Should return empty with only 1 completed period")
    }
}
