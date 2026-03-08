package com.veleda.cyclewise.domain.insights

import com.benasher44.uuid.uuid4
import com.veleda.cyclewise.domain.insights.generators.*
import com.veleda.cyclewise.domain.models.DailyEntry
import com.veleda.cyclewise.domain.models.FullDailyLog
import com.veleda.cyclewise.domain.models.Period
import com.veleda.cyclewise.domain.models.Symptom
import com.veleda.cyclewise.domain.models.SymptomCategory
import com.veleda.cyclewise.domain.models.SymptomLog
import com.veleda.cyclewise.testutil.TestData
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.ExperimentalTime

class InsightEngineTest {

    private lateinit var insightEngine: InsightEngine
    private lateinit var periods: List<Period>
    private lateinit var logs: List<FullDailyLog>
    private lateinit var symptomLib: List<Symptom>

    @BeforeTest
    fun setUp() {
        insightEngine = InsightEngine(
            listOf(
                CycleLengthAverageGenerator(),
                CycleLengthTrendGenerator(),
                MoodPhasePatternGenerator(),
                NextPeriodPredictionGenerator(),
                SymptomPhasePatternGenerator(),
                SymptomRecurrenceGenerator()
            )
        )
        setupTestData()
    }

    @OptIn(ExperimentalTime::class)
    private fun setupTestData() {
        symptomLib = listOf(
            Symptom("symptom-headache", "Headache", SymptomCategory.PAIN, TestData.INSTANT),
            Symptom("symptom-cramps", "Cramps", SymptomCategory.PAIN, TestData.INSTANT),
            Symptom("symptom-bloating", "Bloating", SymptomCategory.DIGESTIVE, TestData.INSTANT)
        )

        val today = LocalDate(2025, 6, 15)
        val tempPeriods = mutableListOf<Period>()
        var lastPeriodStart = today.minus(10, DateTimeUnit.DAY)
        val cycleLengths = listOf(28, 30, 29, 28)

        for (i in 0..4) {
            val endDate = if (i == 0) null else lastPeriodStart.plus(4, DateTimeUnit.DAY)
            tempPeriods.add(Period("period-$i", lastPeriodStart, endDate, TestData.INSTANT, TestData.INSTANT))
            if (i < cycleLengths.size) {
                lastPeriodStart = lastPeriodStart.minus(cycleLengths[i], DateTimeUnit.DAY)
            }
        }
        periods = tempPeriods

        val tempLogs = mutableListOf<FullDailyLog>()
        val chronologicalCompletedPeriods = periods.filter { it.endDate != null }.reversed()
        val periodPairs = chronologicalCompletedPeriods.zipWithNext()

        for ((currentPeriod, nextPeriod) in periodPairs) {
            val cycleLength = currentPeriod.startDate.daysUntil(nextPeriod.startDate)

            for (day in (cycleLength - 3)..(cycleLength - 1)) {
                val logDate = currentPeriod.startDate.plus(day, DateTimeUnit.DAY)
                val entryId = uuid4().toString()
                tempLogs.add(FullDailyLog(
                    entry = DailyEntry(
                        id = entryId,
                        entryDate = logDate,
                        dayInCycle = day + 1,
                        createdAt = TestData.INSTANT,
                        updatedAt = TestData.INSTANT
                    ),
                    symptomLogs = listOf(SymptomLog(uuid4().toString(), entryId, symptomLib.find { it.name == "Bloating" }!!.id, 4, TestData.INSTANT))
                ))
            }

            val entryIdDay1 = uuid4().toString()
            tempLogs.add(FullDailyLog(
                entry = DailyEntry(
                    id = entryIdDay1,
                    entryDate = currentPeriod.startDate,
                    dayInCycle = 1,
                    createdAt = TestData.INSTANT,
                    updatedAt = TestData.INSTANT
                ),
                symptomLogs = listOf(SymptomLog(uuid4().toString(), entryIdDay1, symptomLib.find { it.name == "Cramps" }!!.id, 3, TestData.INSTANT))
            ))
        }
        logs = tempLogs
    }

    /** Helper to extract raw [Insight] list from scored results. */
    private fun List<ScoredInsight>.insights(): List<Insight> = map { it.insight }

    @Test
    fun generateInsights_WHEN_sufficientData_THEN_allTypesPresent() {
        // ACT
        val insights = insightEngine.generateInsights(periods, logs, symptomLib, topSymptomsCount = 2).insights()

        // ASSERT
        assertTrue(insights.filterIsInstance<NextPeriodPrediction>().isNotEmpty(), "Missing NextPeriodPrediction")
        assertTrue(insights.filterIsInstance<CycleLengthAverage>().isNotEmpty(), "Missing CycleLengthAverage")
        assertTrue(insights.filterIsInstance<TopSymptomsInsight>().isNotEmpty(), "Missing TopSymptomsInsight")
        assertTrue(insights.filterIsInstance<SymptomPhasePattern>().any { it.symptomName == "Bloating" }, "Missing SymptomPhasePattern for 'Bloating'")

        val prediction = insights.filterIsInstance<NextPeriodPrediction>().first()
        assertEquals(LocalDate(2025, 7, 4), prediction.predictedDate, "Predicted date should be calculated correctly")

        val average = insights.filterIsInstance<CycleLengthAverage>().first()
        assertEquals(29.0, average.averageDays, 0.1, "Average cycle length should be correct")
    }

    @Test
    fun generateInsights_WHEN_generated_THEN_sortedByRelevanceDesc() {
        // ACT
        val scored = insightEngine.generateInsights(periods, logs, symptomLib, topSymptomsCount = 2)

        // ASSERT
        val scores = scored.map { it.relevanceScore }
        assertTrue(scores.zipWithNext { a, b -> a >= b }.all { it }, "Insights should be sorted by relevance descending")
    }

    @Test
    fun generateInsights_WHEN_insufficientPeriods_THEN_omitsCycleInsights() {
        // ARRANGE
        val insufficientPeriods = periods.take(2)

        // ACT
        val insights = insightEngine.generateInsights(insufficientPeriods, logs, symptomLib, topSymptomsCount = 2).insights()

        // ASSERT
        assertTrue(insights.none { it is NextPeriodPrediction || it is CycleLengthAverage }, "Cycle-based insights should be omitted")
        assertTrue(insights.filterIsInstance<TopSymptomsInsight>().isNotEmpty(), "Symptom-based insights should still be generated")
    }

    @Test
    fun generateInsights_WHEN_insufficientLogs_THEN_omitsSymptomInsights() {
        // ARRANGE
        val fewLogs = logs.take(2)

        // ACT
        val insights = insightEngine.generateInsights(periods, fewLogs, symptomLib, topSymptomsCount = 2).insights()

        // ASSERT
        assertTrue(insights.filterIsInstance<CycleLengthAverage>().isNotEmpty(), "Cycle insights should still be generated")
        assertTrue(insights.none { it is TopSymptomsInsight || it is SymptomPhasePattern }, "Symptom insights should be omitted due to lack of data")
    }

    @Test
    fun generateInsights_WHEN_topCountIsOne_THEN_returnsOnlyTopSymptom() {
        // ACT
        val insights = insightEngine.generateInsights(periods, logs, symptomLib, topSymptomsCount = 1).insights()

        // ASSERT
        val topSymptomsInsight = insights.filterIsInstance<TopSymptomsInsight>().firstOrNull()
        assertNotNull(topSymptomsInsight, "TopSymptomsInsight should be generated")
        assertEquals(1, topSymptomsInsight.topSymptoms.size, "Should only return the single top symptom")
        assertEquals("Bloating", topSymptomsInsight.topSymptoms.first(), "The top symptom should be 'Bloating'")
    }

    @Test
    fun generateInsights_WHEN_noData_THEN_returnsEmptyList() {
        // ACT
        val insights = insightEngine.generateInsights(emptyList(), emptyList(), emptyList(), topSymptomsCount = 3)

        // ASSERT
        assertTrue(insights.isEmpty(), "Expected no insights when no data is provided")
    }
}
