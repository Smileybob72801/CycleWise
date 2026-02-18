package com.veleda.cyclewise.domain.insights.generators

import com.veleda.cyclewise.domain.insights.CycleLengthTrend
import com.veleda.cyclewise.domain.models.Period
import com.veleda.cyclewise.testutil.TestData
import com.veleda.cyclewise.testutil.buildPeriod
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.time.ExperimentalTime

/**
 * Boundary tests for the lowered activation threshold of [CycleLengthTrendGenerator].
 *
 * The generator now requires a minimum of 3 completed periods (2 inter-cycle durations)
 * to produce a [CycleLengthTrend] insight.
 */
@OptIn(ExperimentalTime::class)
class CycleLengthTrendGeneratorTest {

    private val generator = CycleLengthTrendGenerator()

    /**
     * Builds an [InsightData] snapshot containing only periods (no logs or symptoms needed).
     */
    private fun insightDataWithPeriods(periods: List<Period>) = InsightData(
        allPeriods = periods,
        allLogs = emptyList(),
        symptomLibrary = emptyList(),
        averageCycleLength = null,
        topSymptomsCount = 3
    )

    @Test
    fun generate_WHEN_exactlyThreeCompletedPeriods_THEN_producesTrendInsight() {
        // ARRANGE - 3 completed periods (descending), cycle lengths 30 and 28 days
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
                startDate = TestData.DATE.minus(58, DateTimeUnit.DAY),
                endDate = TestData.DATE.minus(54, DateTimeUnit.DAY)
            )
        )

        // ACT
        val insights = generator.generate(insightDataWithPeriods(periods))

        // ASSERT
        assertTrue(insights.isNotEmpty(), "Should produce a trend insight with exactly 3 completed periods")
        assertTrue(insights.first() is CycleLengthTrend, "Insight should be a CycleLengthTrend")
    }

    @Test
    fun generate_WHEN_twoCompletedPeriods_THEN_returnsEmpty() {
        // ARRANGE - only 2 completed periods (below the threshold of 3)
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

        // ACT
        val insights = generator.generate(insightDataWithPeriods(periods))

        // ASSERT
        assertTrue(insights.isEmpty(), "Should return empty with only 2 completed periods")
    }
}
