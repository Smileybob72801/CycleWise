package com.veleda.cyclewise.domain.insights.generators

import com.veleda.cyclewise.domain.insights.SymptomPhasePattern
import com.veleda.cyclewise.testutil.TestData
import com.veleda.cyclewise.testutil.buildDailyEntry
import com.veleda.cyclewise.testutil.buildFullDailyLog
import com.veleda.cyclewise.testutil.buildPeriod
import com.veleda.cyclewise.testutil.buildSymptom
import com.veleda.cyclewise.testutil.buildSymptomLog
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.time.ExperimentalTime

/**
 * Boundary tests for the lowered activation threshold of [SymptomPhasePatternGenerator].
 *
 * The generator now requires a minimum of 2 completed periods (1 full cycle) and a
 * significance count of >= 1 (down from >= 3) to produce [SymptomPhasePattern] insights.
 */
@OptIn(ExperimentalTime::class)
class SymptomPhasePatternGeneratorTest {

    private val generator = SymptomPhasePatternGenerator()

    @Test
    fun generate_WHEN_exactlyTwoCompletedPeriods_THEN_producesPattern() {
        // ARRANGE - 2 completed periods forming 1 cycle pair with a symptom log
        val p1Start = TestData.DATE.minus(28, DateTimeUnit.DAY)
        val p2Start = TestData.DATE

        val symptom = buildSymptom(id = "symptom-headache", name = "Headache")

        val periods = listOf(
            buildPeriod(startDate = p2Start, endDate = p2Start.plus(4, DateTimeUnit.DAY)),
            buildPeriod(startDate = p1Start, endDate = p1Start.plus(4, DateTimeUnit.DAY))
        )

        val entryId = "entry-day2"
        val logs = listOf(
            buildFullDailyLog(
                entry = buildDailyEntry(
                    id = entryId,
                    entryDate = p1Start.plus(1, DateTimeUnit.DAY),
                    dayInCycle = 2
                ),
                symptomLogs = listOf(
                    buildSymptomLog(entryId = entryId, symptomId = symptom.id)
                )
            )
        )

        val data = InsightData(
            allPeriods = periods,
            allLogs = logs,
            symptomLibrary = listOf(symptom),
            averageCycleLength = 28.0,
            topSymptomsCount = 3
        )

        // ACT
        val insights = generator.generate(data)

        // ASSERT
        assertTrue(insights.isNotEmpty(), "Should produce a symptom phase pattern with 2 completed periods")
        assertTrue(
            insights.all { it is SymptomPhasePattern },
            "All insights should be SymptomPhasePattern"
        )
    }

    @Test
    fun generate_WHEN_oneCompletedPeriod_THEN_returnsEmpty() {
        // ARRANGE - only 1 completed period (below threshold of 2)
        val symptom = buildSymptom(id = "symptom-headache", name = "Headache")

        val periods = listOf(
            buildPeriod(
                startDate = TestData.DATE,
                endDate = TestData.DATE.plus(4, DateTimeUnit.DAY)
            )
        )

        val data = InsightData(
            allPeriods = periods,
            allLogs = emptyList(),
            symptomLibrary = listOf(symptom),
            averageCycleLength = null,
            topSymptomsCount = 3
        )

        // ACT
        val insights = generator.generate(data)

        // ASSERT
        assertTrue(insights.isEmpty(), "Should return empty with only 1 completed period")
    }
}
