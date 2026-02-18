package com.veleda.cyclewise.domain.insights.generators

import com.veleda.cyclewise.domain.insights.TopSymptomsInsight
import com.veleda.cyclewise.testutil.TestData
import com.veleda.cyclewise.testutil.buildDailyEntry
import com.veleda.cyclewise.testutil.buildFullDailyLog
import com.veleda.cyclewise.testutil.buildSymptom
import com.veleda.cyclewise.testutil.buildSymptomLog
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.plus
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.time.ExperimentalTime

/**
 * Boundary tests for the lowered activation threshold of [SymptomRecurrenceGenerator].
 *
 * The generator now requires a minimum of 3 total symptom log entries (down from 10)
 * to produce a [TopSymptomsInsight].
 */
@OptIn(ExperimentalTime::class)
class SymptomRecurrenceGeneratorTest {

    private val generator = SymptomRecurrenceGenerator()

    @Test
    fun generate_WHEN_exactlyThreeSymptomLogs_THEN_producesTopSymptoms() {
        // ARRANGE - 3 symptom logs across 3 daily entries (at the new minimum)
        val symptom = buildSymptom(id = "symptom-cramps", name = "Cramps")

        val logs = (1..3).map { i ->
            val entryId = "entry-$i"
            buildFullDailyLog(
                entry = buildDailyEntry(
                    id = entryId,
                    entryDate = TestData.DATE.plus(i, DateTimeUnit.DAY),
                    dayInCycle = i
                ),
                symptomLogs = listOf(
                    buildSymptomLog(entryId = entryId, symptomId = symptom.id)
                )
            )
        }

        val data = InsightData(
            allPeriods = emptyList(),
            allLogs = logs,
            symptomLibrary = listOf(symptom),
            averageCycleLength = null,
            topSymptomsCount = 3
        )

        // ACT
        val insights = generator.generate(data)

        // ASSERT
        assertTrue(insights.isNotEmpty(), "Should produce a top symptoms insight with exactly 3 symptom logs")
        assertTrue(insights.first() is TopSymptomsInsight, "Insight should be TopSymptomsInsight")
    }

    @Test
    fun generate_WHEN_twoSymptomLogs_THEN_returnsEmpty() {
        // ARRANGE - only 2 symptom logs (below threshold of 3)
        val symptom = buildSymptom(id = "symptom-cramps", name = "Cramps")

        val logs = (1..2).map { i ->
            val entryId = "entry-$i"
            buildFullDailyLog(
                entry = buildDailyEntry(
                    id = entryId,
                    entryDate = TestData.DATE.plus(i, DateTimeUnit.DAY),
                    dayInCycle = i
                ),
                symptomLogs = listOf(
                    buildSymptomLog(entryId = entryId, symptomId = symptom.id)
                )
            )
        }

        val data = InsightData(
            allPeriods = emptyList(),
            allLogs = logs,
            symptomLibrary = listOf(symptom),
            averageCycleLength = null,
            topSymptomsCount = 3
        )

        // ACT
        val insights = generator.generate(data)

        // ASSERT
        assertTrue(insights.isEmpty(), "Should return empty with only 2 symptom logs")
    }
}
