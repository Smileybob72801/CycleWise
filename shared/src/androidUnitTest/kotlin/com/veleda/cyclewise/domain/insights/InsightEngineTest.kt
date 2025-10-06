package com.veleda.cyclewise.domain.insights

import com.veleda.cyclewise.domain.models.Cycle
import com.veleda.cyclewise.domain.models.DailyEntry
import com.veleda.cyclewise.domain.models.FullDailyLog
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

    // We now need FullDailyLog objects, not just SymptomLogs
    @OptIn(ExperimentalTime::class)
    private val fullLogs = listOf(
        FullDailyLog(
            entry = DailyEntry("entry-1", "cycle-2", LocalDate(2025, 2, 2), 2, createdAt = Clock.System.now(), updatedAt = Clock.System.now()),
            symptomLogs = listOf(SymptomLog("log-1", "entry-1", "symptom-1", 3, Clock.System.now()))
        ),
        FullDailyLog(
            entry = DailyEntry("entry-2", "cycle-3", LocalDate(2025, 3, 1), 2, createdAt = Clock.System.now(), updatedAt = Clock.System.now()),
            symptomLogs = listOf(SymptomLog("log-2", "entry-2", "symptom-2", 4, Clock.System.now()))
        ),
        FullDailyLog(
            entry = DailyEntry("entry-3", "cycle-4", LocalDate(2025, 3, 31), 2, createdAt = Clock.System.now(), updatedAt = Clock.System.now()),
            symptomLogs = listOf(SymptomLog("log-3", "entry-3", "symptom-1", 5, Clock.System.now())) // Headache is most common
        )
    )

    @OptIn(ExperimentalTime::class)
    private val cycles = listOf(
        Cycle("cycle-4", LocalDate(2025, 3, 30), null, Clock.System.now(), Clock.System.now()),
        Cycle("cycle-3", LocalDate(2025, 2, 28), LocalDate(2025, 3, 4), Clock.System.now(), Clock.System.now()),
        Cycle("cycle-2", LocalDate(2025, 1, 31), LocalDate(2025, 2, 4), Clock.System.now(), Clock.System.now()),
        Cycle("cycle-1", LocalDate(2025, 1, 1), LocalDate(2025, 1, 5), Clock.System.now(), Clock.System.now())
    )

    @BeforeTest
    fun setUp() {
        insightEngine = InsightEngine()
    }

    @Test
    fun generateInsights_WHEN_sufficientDataExists_THEN_returnsAllInsightsSortedByPriority() {
        // ACT
        val insights = insightEngine.generateInsights(cycles, fullLogs, symptomLib)

        // ASSERT
        assertTrue(insights.any { it is NextPeriodPrediction }, "Missing NextPeriodPrediction")
        assertTrue(insights.any { it is CycleLengthAverage }, "Missing CycleLengthAverage")
        assertTrue(insights.any { it is SymptomRecurrence }, "Missing SymptomRecurrence")

        val cycleInsight = insights.filterIsInstance<CycleLengthAverage>().first()
        assertEquals(29.0, cycleInsight.averageDays, 0.1)

        val predictionInsight = insights.filterIsInstance<NextPeriodPrediction>().first()
        assertEquals(LocalDate(2025, 4, 28), predictionInsight.predictedDate)
    }

    @OptIn(ExperimentalTime::class)
    @Test
    fun generateInsights_WHEN_notEnoughCompletedCycles_THEN_omitsPredictionAndAverageInsights() {
        // ARRANGE
        val insufficientCycles = listOf(
            Cycle("cycle-2", LocalDate(2025, 2, 1), null, Clock.System.now(), Clock.System.now()),
            Cycle("cycle-1", LocalDate(2025, 1, 1), LocalDate(2025, 1, 5), Clock.System.now(), Clock.System.now())
        )

        // ACT
        val insights = insightEngine.generateInsights(insufficientCycles, fullLogs, symptomLib)

        // ASSERT
        assertTrue(insights.none { it is NextPeriodPrediction || it is CycleLengthAverage })
        assertTrue(insights.any { it is SymptomRecurrence }, "Symptom insight should still be generated")
    }

    @Test
    fun generateInsights_WHEN_noLogs_THEN_omitsSymptomInsights() {
        // ARRANGE
        val noLogs = emptyList<FullDailyLog>()

        // ACT
        val insights = insightEngine.generateInsights(cycles, noLogs, symptomLib)

        // ASSERT
        assertTrue(insights.any { it is NextPeriodPrediction })
        assertTrue(insights.any { it is CycleLengthAverage })
        assertTrue(insights.none { it is SymptomRecurrence || it is RecurringPeriodSymptom })
    }

    @Test
    fun generateInsights_WHEN_noData_THEN_returnsEmptyList() {
        // ACT
        val insights = insightEngine.generateInsights(emptyList(), emptyList(), emptyList())

        // ASSERT
        assertTrue(insights.isEmpty(), "Expected no insights when no data is provided")
    }
}