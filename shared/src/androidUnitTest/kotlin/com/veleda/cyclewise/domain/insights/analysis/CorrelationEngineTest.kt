package com.veleda.cyclewise.domain.insights.analysis

import com.veleda.cyclewise.domain.models.FlowIntensity
import com.veleda.cyclewise.testutil.buildDailyEntry
import com.veleda.cyclewise.testutil.buildFullDailyLog
import com.veleda.cyclewise.testutil.buildPeriodLog
import com.veleda.cyclewise.testutil.buildSymptomLog
import com.veleda.cyclewise.testutil.buildWaterIntake
import kotlinx.datetime.LocalDate
import kotlin.time.ExperimentalTime
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalTime::class)
class CorrelationEngineTest {

    private val engine = CorrelationEngine()

    // ── spearmanCorrelation ─────────────────────────────────────────────

    @Test
    fun `spearmanCorrelation WHEN perfectPositive THEN returns1`() {
        val x = listOf(1.0, 2.0, 3.0, 4.0, 5.0)
        val y = listOf(1.0, 2.0, 3.0, 4.0, 5.0)
        val result = engine.spearmanCorrelation(x, y)
        assertNotNull(result)
        assertEquals(1.0, result, 0.001)
    }

    @Test
    fun `spearmanCorrelation WHEN perfectNegative THEN returnsMinus1`() {
        val x = listOf(1.0, 2.0, 3.0, 4.0, 5.0)
        val y = listOf(5.0, 4.0, 3.0, 2.0, 1.0)
        val result = engine.spearmanCorrelation(x, y)
        assertNotNull(result)
        assertEquals(-1.0, result, 0.001)
    }

    @Test
    fun `spearmanCorrelation WHEN tiedValues THEN handlesWithAverageRank`() {
        val x = listOf(1.0, 2.0, 2.0, 4.0, 5.0)
        val y = listOf(1.0, 3.0, 2.0, 4.0, 5.0)
        val result = engine.spearmanCorrelation(x, y)
        assertNotNull(result)
        assertTrue(result > 0.8, "Expected strong positive correlation with ties, got $result")
    }

    @Test
    fun `spearmanCorrelation WHEN tooFewValues THEN returnsNull`() {
        assertNull(engine.spearmanCorrelation(listOf(1.0), listOf(2.0)))
    }

    @Test
    fun `spearmanCorrelation WHEN zeroVariance THEN returnsNull`() {
        val x = listOf(3.0, 3.0, 3.0, 3.0, 3.0)
        val y = listOf(1.0, 2.0, 3.0, 4.0, 5.0)
        assertNull(engine.spearmanCorrelation(x, y))
    }

    // ── computeAllCorrelations ──────────────────────────────────────────

    @Test
    fun `computeAllCorrelations WHEN insufficientData THEN returnsEmpty`() {
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
        val results = engine.computeAllCorrelations(logs, emptyList(), minSampleSize = 30)
        assertTrue(results.isEmpty())
    }

    @Test
    fun `computeAllCorrelations WHEN perfectCorrelation THEN detectsIt`() {
        // Build 35 logs where mood = energy (perfect correlation)
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

        val results = engine.computeAllCorrelations(logs, emptyList(), minSampleSize = 30)

        val moodEnergy = results.find {
            (it.variableA == MetricType.MOOD && it.variableB == MetricType.ENERGY) ||
                (it.variableA == MetricType.ENERGY && it.variableB == MetricType.MOOD)
        }
        assertNotNull(moodEnergy, "Should detect mood-energy correlation")
        assertEquals(CorrelationDirection.POSITIVE, moodEnergy.direction)
        assertTrue(moodEnergy.coefficient > 0.9)
    }

    @Test
    fun `computeAllCorrelations WHEN weakCorrelation THEN filtersOut`() {
        // Build 35 logs with near-random relationship between mood and energy
        val logs = (1..35).map { i ->
            buildFullDailyLog(
                entry = buildDailyEntry(
                    entryDate = LocalDate.fromEpochDays(LocalDate(2025, 1, 1).toEpochDays() + i),
                    dayInCycle = i,
                    moodScore = (i % 5) + 1,
                    energyLevel = ((i * 3 + 7) % 5) + 1,
                ),
            )
        }
        val results = engine.computeAllCorrelations(logs, emptyList(), minSampleSize = 30)
        // Weak or non-existent correlations should be filtered out (|coef| < 0.3)
        results.forEach { result ->
            assertTrue(
                kotlin.math.abs(result.coefficient) >= CorrelationEngine.MIN_COEFFICIENT,
                "All returned correlations should meet minimum threshold"
            )
        }
    }

    @Test
    fun `computeAllCorrelations WHEN resultsSorted THEN descendingByAbsCoefficient`() {
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
        val results = engine.computeAllCorrelations(logs, emptyList(), minSampleSize = 30)
        if (results.size >= 2) {
            for (i in 0 until results.size - 1) {
                assertTrue(
                    kotlin.math.abs(results[i].coefficient) >= kotlin.math.abs(results[i + 1].coefficient),
                    "Results should be sorted by |coefficient| descending"
                )
            }
        }
    }

    // ── Strength classification ─────────────────────────────────────────

    @Test
    fun `strengthClassification WHEN strongCorrelation THEN classifiesAsStrong`() {
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
        val results = engine.computeAllCorrelations(logs, emptyList(), minSampleSize = 30)
        val moodEnergy = results.find {
            it.variableA == MetricType.MOOD && it.variableB == MetricType.ENERGY
        }
        assertNotNull(moodEnergy)
        assertEquals(CorrelationStrength.STRONG, moodEnergy.strength)
    }
}
