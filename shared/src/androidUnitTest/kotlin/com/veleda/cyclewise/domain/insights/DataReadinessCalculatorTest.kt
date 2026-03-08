package com.veleda.cyclewise.domain.insights

import com.veleda.cyclewise.testutil.TestData
import com.veleda.cyclewise.testutil.buildPeriod
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.ExperimentalTime

/**
 * Unit tests for [DataReadinessCalculator].
 *
 * Verifies readiness computation for each [InsightCategory] based on period
 * counts and threshold requirements.
 */
@OptIn(ExperimentalTime::class)
class DataReadinessCalculatorTest {

    private val calculator = DataReadinessCalculator()

    @Test
    fun calculate_WHEN_noPeriods_THEN_noCategoriesReady() {
        // GIVEN - no periods at all
        // WHEN
        val readiness = calculator.calculate(emptyList())

        // THEN
        assertTrue(
            readiness.none { it.isReady },
            "No categories should be ready with zero periods"
        )
        val summary = readiness.first { it.category == InsightCategory.SUMMARY }
        assertEquals(1, summary.periodsRequired, "Summary requires 1 period")
        assertEquals(1, summary.periodsRemaining, "Should need 1 more period for summary")
    }

    @Test
    fun calculate_WHEN_onePeriod_THEN_onlySummaryReady() {
        // GIVEN - 1 period (ongoing, no endDate)
        val periods = listOf(
            buildPeriod(startDate = TestData.DATE)
        )

        // WHEN
        val readiness = calculator.calculate(periods)

        // THEN
        val summary = readiness.first { it.category == InsightCategory.SUMMARY }
        assertTrue(summary.isReady, "Summary should be ready with 1 period")
        assertEquals(0, summary.periodsRemaining, "No more periods needed for summary")

        val prediction = readiness.first { it.category == InsightCategory.PREDICTION }
        assertFalse(prediction.isReady, "Prediction should not be ready with 0 completed periods")
        assertEquals(2, prediction.periodsRemaining, "Should need 2 more completed periods for prediction")
    }

    @Test
    fun calculate_WHEN_threeCompletedPeriods_THEN_allCategoriesReady() {
        // GIVEN - 3 completed periods (meets all thresholds including TREND at 3)
        val periods = listOf(
            buildPeriod(
                startDate = TestData.DATE,
                endDate = TestData.DATE.plus(4, DateTimeUnit.DAY)
            ),
            buildPeriod(
                startDate = TestData.DATE.minus(28, DateTimeUnit.DAY),
                endDate = TestData.DATE.minus(24, DateTimeUnit.DAY)
            ),
            buildPeriod(
                startDate = TestData.DATE.minus(56, DateTimeUnit.DAY),
                endDate = TestData.DATE.minus(52, DateTimeUnit.DAY)
            )
        )

        // WHEN
        val readiness = calculator.calculate(periods)

        // THEN
        assertTrue(
            readiness.all { it.isReady },
            "All categories should be ready with 3 completed periods"
        )
        readiness.forEach { dr ->
            assertEquals(0, dr.periodsRemaining, "${dr.category} should have 0 periods remaining")
        }
    }

    @Test
    fun calculate_WHEN_twoCompletedPeriods_THEN_trendNotReady() {
        // GIVEN - 2 completed periods (TREND requires 3)
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

        // WHEN
        val readiness = calculator.calculate(periods)

        // THEN
        val trend = readiness.first { it.category == InsightCategory.TREND }
        assertFalse(trend.isReady, "Trend should not be ready with only 2 completed periods")
        assertEquals(1, trend.periodsRemaining, "Should need 1 more completed period for trend")

        val pattern = readiness.first { it.category == InsightCategory.PATTERN }
        assertTrue(pattern.isReady, "Pattern should be ready with 2 completed periods")

        val prediction = readiness.first { it.category == InsightCategory.PREDICTION }
        assertTrue(prediction.isReady, "Prediction should be ready with 2 completed periods")
    }

    @Test
    fun calculate_WHEN_resultReturned_THEN_sortedNotReadyFirst() {
        // GIVEN - 2 completed periods (some ready, some not)
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

        // WHEN
        val readiness = calculator.calculate(periods)

        // THEN - not-ready items should come before ready items
        val readyIndices = readiness.mapIndexed { index, dr -> index to dr.isReady }
        val firstReadyIndex = readyIndices.firstOrNull { it.second }?.first ?: readiness.size
        val lastNotReadyIndex = readyIndices.lastOrNull { !it.second }?.first ?: -1

        assertTrue(
            lastNotReadyIndex < firstReadyIndex,
            "Not-ready items should be sorted before ready items"
        )
    }
}
