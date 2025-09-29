package com.veleda.cyclewise.domain.insights

import com.veleda.cyclewise.domain.models.Cycle
import com.veleda.cyclewise.domain.models.Symptom
import com.veleda.cyclewise.domain.models.SymptomCategory
import com.veleda.cyclewise.domain.models.SymptomLog
import kotlinx.datetime.LocalDate
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class InsightEngineTest {

    private lateinit var insightEngine: InsightEngine

    // --- Test Data ---
    @OptIn(ExperimentalTime::class)
    private val symptomLib = listOf(
        Symptom("symptom-1", "Headache", SymptomCategory.PAIN, Clock.System.now()),
        Symptom("symptom-2", "Cramps", SymptomCategory.PAIN, Clock.System.now())
    )
    @OptIn(ExperimentalTime::class)
    private val symptomLogs = listOf(
        SymptomLog("log-1", "entry-1", "symptom-1", 3, Clock.System.now()),
        SymptomLog("log-2", "entry-2", "symptom-2", 4, Clock.System.now()),
        SymptomLog("log-3", "entry-3", "symptom-1", 5, Clock.System.now()) // Headache is most common
    )

    // Realistic test data. Periods are a few days long.
    // List is sorted descending by start date, as it would be from the repository.
    @OptIn(ExperimentalTime::class)
    private val cycles = listOf(
        // Latest cycle (ongoing)
        Cycle("cycle-4", LocalDate(2025, 3, 30), null, Clock.System.now(), Clock.System.now()),
        // Completed cycle. Duration from start to next start = 30 days.
        Cycle("cycle-3", LocalDate(2025, 2, 28), LocalDate(2025, 3, 4), Clock.System.now(), Clock.System.now()),
        // Completed cycle. Duration from start to next start = 28 days.
        Cycle("cycle-2", LocalDate(2025, 1, 31), LocalDate(2025, 2, 4), Clock.System.now(), Clock.System.now()),
        // Completed cycle.
        Cycle("cycle-1", LocalDate(2025, 1, 1), LocalDate(2025, 1, 5), Clock.System.now(), Clock.System.now())
    )

    @BeforeTest
    fun setUp() {
        insightEngine = InsightEngine()
    }

    @Test
    fun generateInsights_WHEN_sufficientDataExists_THEN_returnsAllInsightsSortedByPriority() {
        // ACT
        val insights = insightEngine.generateInsights(cycles, symptomLogs, symptomLib)

        // ASSERT
        assertEquals(3, insights.size)
        // Check sorting: Prediction (110), Average (100), Symptom (90)
        assertTrue(insights[0] is NextPeriodPrediction)
        assertTrue(insights[1] is CycleLengthAverage)
        assertTrue(insights[2] is SymptomRecurrence)

        // Test average cycle length calculation:
        // Duration 1 = (Jan 29 - Jan 1) = 28 days.
        // Duration 2 = (Feb 28 - Jan 29) = 30 days.
        // Average = (28 + 30) / 2 = 29 days.
        val cycleInsight = insights[1] as CycleLengthAverage
        assertEquals(29.0, cycleInsight.averageDays)

        // Test period prediction calculation:
        // Prediction = Start of last period (March 30) + 29 days = April 28.
        val predictionInsight = insights[0] as NextPeriodPrediction
        assertEquals(LocalDate(2025, 4, 28), predictionInsight.predictedDate)
    }

    @OptIn(ExperimentalTime::class)
    @Test
    fun generateInsights_WHEN_notEnoughCompletedCycles_THEN_omitsPredictionAndAverageInsights() {
        // ARRANGE: Only one completed cycle, which is not enough to calculate a duration.
        val insufficientCycles = listOf(
            Cycle("cycle-2", LocalDate(2025, 2, 1), null, Clock.System.now(), Clock.System.now()),
            Cycle("cycle-1", LocalDate(2025, 1, 1), LocalDate(2025, 1, 5), Clock.System.now(), Clock.System.now())
        )

        // ACT
        val insights = insightEngine.generateInsights(insufficientCycles, symptomLogs, symptomLib)

        // ASSERT
        assertEquals(1, insights.size, "Should only generate symptom insight, as cycle insights require at least two completed periods")
        assertTrue(insights[0] is SymptomRecurrence)
    }

    @Test
    fun generateInsights_WHEN_noSymptomLogs_THEN_omitsSymptomRecurrenceInsight() {
        // ARRANGE: Provide an empty list of logs
        val noLogs = emptyList<SymptomLog>()

        // ACT
        val insights = insightEngine.generateInsights(cycles, noLogs, symptomLib)

        // ASSERT
        assertEquals(2, insights.size)
        assertTrue(insights.any { it is NextPeriodPrediction }, "Should generate prediction insight")
        assertTrue(insights.any { it is CycleLengthAverage }, "Should generate cycle length insight")
    }

    @Test
    fun generateInsights_WHEN_noData_THEN_returnsEmptyList() {
        // ACT
        val insights = insightEngine.generateInsights(emptyList(), emptyList(), emptyList())

        // ASSERT
        assertTrue(insights.isEmpty(), "Expected no insights when no data is provided")
    }
}