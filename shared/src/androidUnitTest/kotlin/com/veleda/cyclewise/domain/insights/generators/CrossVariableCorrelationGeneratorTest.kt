package com.veleda.cyclewise.domain.insights.generators

import com.veleda.cyclewise.domain.insights.CrossVariableCorrelation
import com.veleda.cyclewise.domain.insights.InsightCategory
import com.veleda.cyclewise.domain.insights.analysis.CorrelationDirection
import com.veleda.cyclewise.domain.insights.analysis.CorrelationEngine
import com.veleda.cyclewise.domain.insights.analysis.CorrelationStrength
import com.veleda.cyclewise.domain.insights.analysis.MetricType
import com.veleda.cyclewise.testutil.buildDailyEntry
import com.veleda.cyclewise.testutil.buildFullDailyLog
import kotlinx.datetime.LocalDate
import kotlin.time.ExperimentalTime
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTime::class)
class CrossVariableCorrelationGeneratorTest {

    private val generator = CrossVariableCorrelationGenerator(CorrelationEngine())

    private fun makeInsightData(logs: List<com.veleda.cyclewise.domain.models.FullDailyLog>) =
        InsightData(
            allPeriods = emptyList(),
            allLogs = logs,
            symptomLibrary = emptyList(),
            averageCycleLength = null,
            topSymptomsCount = 5,
        )

    @Test
    fun `generate WHEN insufficientData THEN returnsEmpty`() {
        val logs = (1..10).map { i ->
            buildFullDailyLog(
                entry = buildDailyEntry(
                    entryDate = LocalDate.fromEpochDays(LocalDate(2025, 1, 1).toEpochDays() + i),
                    dayInCycle = i,
                    moodScore = (i % 5) + 1,
                    energyLevel = (i % 4) + 1,
                ),
            )
        }
        val results = generator.generate(makeInsightData(logs))
        assertTrue(results.isEmpty())
    }

    @Test
    fun `generate WHEN strongCorrelation THEN returnsCrossVariableCorrelation`() {
        val logs = (1..35).map { i ->
            val score = (i % 5) + 1
            buildFullDailyLog(
                entry = buildDailyEntry(
                    entryDate = LocalDate.fromEpochDays(LocalDate(2025, 1, 1).toEpochDays() + i),
                    dayInCycle = i,
                    moodScore = score,
                    energyLevel = score,
                ),
            )
        }

        val results = generator.generate(makeInsightData(logs))

        assertTrue(results.isNotEmpty(), "Should produce at least one correlation insight")
        val correlation = results.first() as CrossVariableCorrelation
        assertEquals(InsightCategory.CORRELATION, correlation.category)
        assertEquals(CorrelationDirection.POSITIVE, correlation.direction)
        assertTrue(correlation.coefficient > 0.9)
        assertTrue(correlation.sampleSize >= 30)
    }

    @Test
    fun `generate WHEN results THEN allAreCrossVariableCorrelation`() {
        val logs = (1..40).map { i ->
            val score = (i % 5) + 1
            buildFullDailyLog(
                entry = buildDailyEntry(
                    entryDate = LocalDate.fromEpochDays(LocalDate(2025, 1, 1).toEpochDays() + i),
                    dayInCycle = i,
                    moodScore = score,
                    energyLevel = score,
                    libidoScore = 6 - score,
                ),
            )
        }

        val results = generator.generate(makeInsightData(logs))

        results.forEach { insight ->
            assertTrue(insight is CrossVariableCorrelation, "All results should be CrossVariableCorrelation")
        }
    }

    @Test
    fun `generate WHEN negativeCorrelation THEN detectsNegativeDirection`() {
        val logs = (1..35).map { i ->
            val score = (i % 5) + 1
            buildFullDailyLog(
                entry = buildDailyEntry(
                    entryDate = LocalDate.fromEpochDays(LocalDate(2025, 1, 1).toEpochDays() + i),
                    dayInCycle = i,
                    moodScore = score,
                    energyLevel = 6 - score,
                ),
            )
        }

        val results = generator.generate(makeInsightData(logs))
        val moodEnergy = results.filterIsInstance<CrossVariableCorrelation>().find {
            (it.variableA == MetricType.MOOD && it.variableB == MetricType.ENERGY) ||
                (it.variableA == MetricType.ENERGY && it.variableB == MetricType.MOOD)
        }

        assertTrue(moodEnergy != null, "Should find mood-energy correlation")
        assertEquals(CorrelationDirection.NEGATIVE, moodEnergy.direction)
    }

    @Test
    fun `generate WHEN result THEN hasUniqueId`() {
        val logs = (1..35).map { i ->
            val score = (i % 5) + 1
            buildFullDailyLog(
                entry = buildDailyEntry(
                    entryDate = LocalDate.fromEpochDays(LocalDate(2025, 1, 1).toEpochDays() + i),
                    dayInCycle = i,
                    moodScore = score,
                    energyLevel = score,
                ),
            )
        }

        val results = generator.generate(makeInsightData(logs))
        val ids = results.map { it.id }
        assertEquals(ids.size, ids.toSet().size, "All insight IDs should be unique")
    }
}
