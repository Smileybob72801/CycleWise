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
    @OptIn(ExperimentalTime::class)
    private val cycles = listOf(
        // Completed cycle of 28 days
        Cycle("cycle-1", LocalDate(2025, 1, 1), LocalDate(2025, 1, 28), Clock.System.now(), Clock.System.now()),
        // Completed cycle of 30 days
        Cycle("cycle-2", LocalDate(2025, 1, 29), LocalDate(2025, 2, 27), Clock.System.now(), Clock.System.now()),
        // Ongoing cycle, should be ignored by length calculation
        Cycle("cycle-3", LocalDate(2025, 2, 28), null, Clock.System.now(), Clock.System.now())
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
        assertEquals(2, insights.size)
        // Check sorting: CycleLengthAverage has priority 100, SymptomRecurrence has 90
        assertTrue(insights[0] is CycleLengthAverage)
        assertTrue(insights[1] is SymptomRecurrence)

        val cycleInsight = insights[0] as CycleLengthAverage
        assertEquals(29.0, cycleInsight.averageDays) // (28 + 30) / 2 = 29

        val symptomInsight = insights[1] as SymptomRecurrence
        assertEquals("Headache", symptomInsight.symptomName) // Headache appears twice
    }

    @Test
    fun generateInsights_WHEN_insufficientCycles_THEN_omitsCycleLengthInsight() {
        // ARRANGE: Only provide one completed cycle
        val insufficientCycles = listOf(cycles.first())

        // ACT
        val insights = insightEngine.generateInsights(insufficientCycles, symptomLogs, symptomLib)

        // ASSERT
        assertEquals(1, insights.size)
        assertTrue(insights.first() is SymptomRecurrence, "Should only generate symptom insight")
    }

    @Test
    fun generateInsights_WHEN_noSymptomLogs_THEN_omitsSymptomRecurrenceInsight() {
        // ARRANGE: Provide an empty list of logs
        val noLogs = emptyList<SymptomLog>()

        // ACT
        val insights = insightEngine.generateInsights(cycles, noLogs, symptomLib)

        // ASSERT
        assertEquals(1, insights.size)
        assertTrue(insights.first() is CycleLengthAverage, "Should only generate cycle length insight")
    }

    @Test
    fun generateInsights_WHEN_noData_THEN_returnsEmptyList() {
        // ACT
        val insights = insightEngine.generateInsights(emptyList(), emptyList(), emptyList())

        // ASSERT
        assertTrue(insights.isEmpty(), "Expected no insights when no data is provided")
    }
}