package com.veleda.cyclewise.domain.insights.generators

import com.veleda.cyclewise.domain.insights.NextPeriodPrediction
import com.veleda.cyclewise.testutil.buildPeriod
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class NextPeriodPredictionGeneratorTest {

    private lateinit var generator: NextPeriodPredictionGenerator

    @BeforeTest
    fun setUp() {
        generator = NextPeriodPredictionGenerator()
    }

    @Test
    fun generate_WHEN_averageCycleLengthIsNull_THEN_returnsEmptyList() {
        // ARRANGE
        val data = InsightData(
            allPeriods = listOf(buildPeriod(startDate = LocalDate(2025, 1, 1), endDate = LocalDate(2025, 1, 5))),
            allLogs = emptyList(),
            symptomLibrary = emptyList(),
            averageCycleLength = null,
            topSymptomsCount = 3
        )

        // ACT
        val result = generator.generate(data)

        // ASSERT
        assertTrue(result.isEmpty())
    }

    @Test
    fun generate_WHEN_noPeriodsExist_THEN_returnsEmptyList() {
        // ARRANGE
        val data = InsightData(
            allPeriods = emptyList(),
            allLogs = emptyList(),
            symptomLibrary = emptyList(),
            averageCycleLength = 28.0,
            topSymptomsCount = 3
        )

        // ACT
        val result = generator.generate(data)

        // ASSERT
        assertTrue(result.isEmpty())
    }

    @Test
    fun generate_WHEN_dataIsAvailable_THEN_returnsNextPeriodPrediction() {
        // ARRANGE
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val latestPeriodStart = today.minus(20, DateTimeUnit.DAY)
        val averageCycleLength = 28.0
        val latestPeriod = buildPeriod(
            startDate = latestPeriodStart,
            endDate = latestPeriodStart.plus(5, DateTimeUnit.DAY)
        )
        val data = InsightData(
            allPeriods = listOf(latestPeriod),
            allLogs = emptyList(),
            symptomLibrary = emptyList(),
            averageCycleLength = averageCycleLength,
            topSymptomsCount = 3
        )

        // ACT
        val result = generator.generate(data)

        // ASSERT
        assertEquals(1, result.size)
        val insight = result.first()
        assertIs<NextPeriodPrediction>(insight)
        val expectedDate = latestPeriodStart.plus(28, DateTimeUnit.DAY)
        assertEquals(expectedDate, insight.predictedDate)
        assertEquals(today.daysUntil(expectedDate), insight.daysUntilPrediction)
    }

    @Test
    fun generate_WHEN_averageCycleLengthHasFraction_THEN_roundsToNearestDay() {
        // ARRANGE
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val latestPeriodStart = today.minus(10, DateTimeUnit.DAY)
        val latestPeriod = buildPeriod(
            startDate = latestPeriodStart,
            endDate = latestPeriodStart.plus(4, DateTimeUnit.DAY)
        )
        val data = InsightData(
            allPeriods = listOf(latestPeriod),
            allLogs = emptyList(),
            symptomLibrary = emptyList(),
            averageCycleLength = 28.7,
            topSymptomsCount = 3
        )

        // ACT
        val result = generator.generate(data)

        // ASSERT
        assertEquals(1, result.size)
        val insight = result.first()
        assertIs<NextPeriodPrediction>(insight)
        // 28.7 rounds to 29
        val expectedDate = latestPeriodStart.plus(29, DateTimeUnit.DAY)
        assertEquals(expectedDate, insight.predictedDate)
    }

    @Test
    fun generate_WHEN_multiplePeriodsExist_THEN_usesFirstPeriodInList() {
        // ARRANGE — allPeriods is sorted by startDate descending
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val latestStart = today.minus(10, DateTimeUnit.DAY)
        val olderStart = today.minus(40, DateTimeUnit.DAY)
        val latestPeriod = buildPeriod(startDate = latestStart, endDate = latestStart.plus(4, DateTimeUnit.DAY))
        val olderPeriod = buildPeriod(startDate = olderStart, endDate = olderStart.plus(5, DateTimeUnit.DAY))
        val data = InsightData(
            allPeriods = listOf(latestPeriod, olderPeriod),
            allLogs = emptyList(),
            symptomLibrary = emptyList(),
            averageCycleLength = 30.0,
            topSymptomsCount = 3
        )

        // ACT
        val result = generator.generate(data)

        // ASSERT — uses latestPeriod (first in list)
        assertEquals(1, result.size)
        val insight = result.first()
        assertIs<NextPeriodPrediction>(insight)
        val expectedDate = latestStart.plus(30, DateTimeUnit.DAY)
        assertEquals(expectedDate, insight.predictedDate)
        assertEquals("NEXT_PERIOD_PREDICTION", insight.id)
        assertEquals(110, insight.priority)
    }
}
