package com.veleda.cyclewise.domain.insights.generators

import com.veleda.cyclewise.domain.insights.CycleSummary
import com.veleda.cyclewise.domain.insights.NextPeriodPrediction
import com.veleda.cyclewise.testutil.TestData
import com.veleda.cyclewise.testutil.buildPeriod
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Unit tests for [CycleSummaryGenerator].
 *
 * Verifies cycle summary generation including phase determination, cycle day
 * calculation, and integration with [NextPeriodPrediction] for days-until display.
 */
@OptIn(ExperimentalTime::class)
class CycleSummaryGeneratorTest {

    private val generator = CycleSummaryGenerator()

    @Test
    fun generate_WHEN_periodStartedRecently_THEN_producesCycleSummary() {
        // GIVEN - a period that started today (cycle day 1)
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val periods = listOf(
            buildPeriod(startDate = today, endDate = today.plus(4, DateTimeUnit.DAY))
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
        assertEquals(1, insights.size, "Should produce exactly 1 CycleSummary")
        val summary = insights.first() as CycleSummary
        assertEquals(1, summary.cycleDay, "Cycle day should be 1 on start date")
        assertEquals(28, summary.cycleLength, "Cycle length should use averageCycleLength")
        assertEquals("menstruation", summary.phaseName, "Day 1 should be menstruation phase")
    }

    @Test
    fun generate_WHEN_noPeriods_THEN_returnsEmpty() {
        // GIVEN - no periods at all
        val data = InsightData(
            allPeriods = emptyList(),
            allLogs = emptyList(),
            symptomLibrary = emptyList(),
            averageCycleLength = null,
            topSymptomsCount = 3
        )

        // WHEN
        val insights = generator.generate(data)

        // THEN
        assertTrue(insights.isEmpty(), "Should return empty when no periods exist")
    }

    @Test
    fun generate_WHEN_noAverageCycleLength_THEN_defaultsTo28() {
        // GIVEN - a period exists but no average cycle length
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val periods = listOf(
            buildPeriod(startDate = today)
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
        val summary = insights.first() as CycleSummary
        assertEquals(28, summary.cycleLength, "Should default to 28-day cycle when no average available")
    }

    @Test
    fun generate_WHEN_nextPeriodPredictionAvailable_THEN_includesDaysUntil() {
        // GIVEN - a period and a NextPeriodPrediction in generatedInsights
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val periods = listOf(
            buildPeriod(startDate = today, endDate = today.plus(4, DateTimeUnit.DAY))
        )

        val prediction = NextPeriodPrediction(
            predictedDate = today.plus(25, DateTimeUnit.DAY),
            daysUntilPrediction = 25
        )

        val data = InsightData(
            allPeriods = periods,
            allLogs = emptyList(),
            symptomLibrary = emptyList(),
            averageCycleLength = 28.0,
            topSymptomsCount = 3,
            generatedInsights = listOf(prediction)
        )

        // WHEN
        val insights = generator.generate(data)

        // THEN
        val summary = insights.first() as CycleSummary
        assertEquals(25, summary.daysUntilNextPeriod, "Should include daysUntilNextPeriod from prediction")
    }

    @Test
    fun generate_WHEN_noPredictionAvailable_THEN_daysUntilIsNull() {
        // GIVEN - a period but no NextPeriodPrediction in generatedInsights
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val periods = listOf(
            buildPeriod(startDate = today)
        )

        val data = InsightData(
            allPeriods = periods,
            allLogs = emptyList(),
            symptomLibrary = emptyList(),
            averageCycleLength = 28.0,
            topSymptomsCount = 3,
            generatedInsights = emptyList()
        )

        // WHEN
        val insights = generator.generate(data)

        // THEN
        val summary = insights.first() as CycleSummary
        assertNull(summary.daysUntilNextPeriod, "Should be null when no prediction available")
    }
}
