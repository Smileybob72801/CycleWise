package com.veleda.cyclewise.domain.insights.analysis

import com.veleda.cyclewise.domain.models.FullDailyLog
import com.veleda.cyclewise.domain.models.WaterIntake
import kotlin.math.sqrt

/**
 * Data types that can be correlated against each other.
 */
enum class MetricType {
    MOOD,
    ENERGY,
    LIBIDO,
    WATER_INTAKE,
    FLOW_INTENSITY,
    SYMPTOM_COUNT,
    SYMPTOM_MAX_SEVERITY,
    MEDICATION_COUNT,
    CYCLE_DAY,
}

/**
 * Direction of a correlation.
 */
enum class CorrelationDirection {
    POSITIVE,
    NEGATIVE,
}

/**
 * Qualitative strength of a correlation.
 */
enum class CorrelationStrength {
    WEAK,
    MODERATE,
    STRONG,
}

/**
 * Result of a Spearman rank correlation analysis between two metrics.
 *
 * @property variableA   First metric.
 * @property variableB   Second metric.
 * @property coefficient Spearman rank correlation coefficient (-1.0 to 1.0).
 * @property direction   Whether the correlation is positive or negative.
 * @property strength    Qualitative strength classification.
 * @property sampleSize  Number of paired observations used.
 */
data class CorrelationResult(
    val variableA: MetricType,
    val variableB: MetricType,
    val coefficient: Double,
    val direction: CorrelationDirection,
    val strength: CorrelationStrength,
    val sampleSize: Int,
)

/**
 * Computes Spearman rank correlations between all metric pairs.
 *
 * Only surfaces correlations with |coefficient| >= [MIN_COEFFICIENT] AND
 * sampleSize >= [MIN_SAMPLE_SIZE] to filter out noise.
 */
class CorrelationEngine {

    companion object {
        const val MIN_COEFFICIENT = 0.3
        const val MIN_SAMPLE_SIZE = 30
    }

    /**
     * Computes all significant correlations between metric pairs.
     *
     * @param logs         All daily logs.
     * @param waterIntakes All water intake records.
     * @param minSampleSize Minimum paired observations required (default 30).
     * @return Significant correlation results, sorted by |coefficient| descending.
     */
    fun computeAllCorrelations(
        logs: List<FullDailyLog>,
        waterIntakes: List<WaterIntake>,
        minSampleSize: Int = MIN_SAMPLE_SIZE,
    ): List<CorrelationResult> {
        val waterByDate = waterIntakes.associateBy { it.date }

        val metrics = MetricType.entries.toList()
        val results = mutableListOf<CorrelationResult>()

        for (i in metrics.indices) {
            for (j in i + 1 until metrics.size) {
                val a = metrics[i]
                val b = metrics[j]

                val pairs = logs.mapNotNull { log ->
                    val valA = extractValue(log, a, waterByDate)
                    val valB = extractValue(log, b, waterByDate)
                    if (valA != null && valB != null) Pair(valA, valB) else null
                }

                if (pairs.size < minSampleSize) continue

                val x = pairs.map { it.first }
                val y = pairs.map { it.second }
                val coefficient = spearmanCorrelation(x, y) ?: continue

                if (kotlin.math.abs(coefficient) < MIN_COEFFICIENT) continue

                results.add(
                    CorrelationResult(
                        variableA = a,
                        variableB = b,
                        coefficient = coefficient,
                        direction = if (coefficient > 0) CorrelationDirection.POSITIVE
                        else CorrelationDirection.NEGATIVE,
                        strength = classifyStrength(coefficient),
                        sampleSize = pairs.size,
                    )
                )
            }
        }

        return results.sortedByDescending { kotlin.math.abs(it.coefficient) }
    }

    /**
     * Computes Spearman rank correlation coefficient.
     *
     * Uses the average-rank method for handling tied values.
     *
     * @return coefficient in [-1.0, 1.0], or null if variance is zero.
     */
    internal fun spearmanCorrelation(x: List<Double>, y: List<Double>): Double? {
        if (x.size != y.size || x.size < 2) return null

        val ranksX = computeRanks(x)
        val ranksY = computeRanks(y)

        val n = x.size
        val meanX = ranksX.average()
        val meanY = ranksY.average()

        var numerator = 0.0
        var denomX = 0.0
        var denomY = 0.0

        for (i in 0 until n) {
            val dx = ranksX[i] - meanX
            val dy = ranksY[i] - meanY
            numerator += dx * dy
            denomX += dx * dx
            denomY += dy * dy
        }

        val denom = sqrt(denomX * denomY)
        if (denom == 0.0) return null

        return numerator / denom
    }

    /**
     * Computes average ranks for a list of values, handling ties.
     */
    private fun computeRanks(values: List<Double>): List<Double> {
        val indexed = values.mapIndexed { i, v -> Pair(i, v) }
        val sorted = indexed.sortedBy { it.second }

        val ranks = DoubleArray(values.size)
        var i = 0
        while (i < sorted.size) {
            var j = i
            while (j < sorted.size && sorted[j].second == sorted[i].second) {
                j++
            }
            val avgRank = (i + 1 + j).toDouble() / 2.0
            for (k in i until j) {
                ranks[sorted[k].first] = avgRank
            }
            i = j
        }

        return ranks.toList()
    }

    private fun extractValue(
        log: FullDailyLog,
        metric: MetricType,
        waterByDate: Map<kotlinx.datetime.LocalDate, WaterIntake>,
    ): Double? = when (metric) {
        MetricType.MOOD -> log.entry.moodScore?.toDouble()
        MetricType.ENERGY -> log.entry.energyLevel?.toDouble()
        MetricType.LIBIDO -> log.entry.libidoScore?.toDouble()
        MetricType.WATER_INTAKE -> waterByDate[log.entry.entryDate]?.cups?.toDouble()
        MetricType.FLOW_INTENSITY -> log.periodLog?.flowIntensity?.let {
            when (it) {
                com.veleda.cyclewise.domain.models.FlowIntensity.LIGHT -> 1.0
                com.veleda.cyclewise.domain.models.FlowIntensity.MEDIUM -> 2.0
                com.veleda.cyclewise.domain.models.FlowIntensity.HEAVY -> 3.0
            }
        }
        MetricType.SYMPTOM_COUNT -> log.symptomLogs.size.toDouble().takeIf { it > 0 }
        MetricType.SYMPTOM_MAX_SEVERITY -> log.symptomLogs.maxOfOrNull { it.severity }?.toDouble()
        MetricType.MEDICATION_COUNT -> log.medicationLogs.size.toDouble().takeIf { it > 0 }
        MetricType.CYCLE_DAY -> log.entry.dayInCycle.toDouble().takeIf { it > 0 }
    }

    @Suppress("MagicNumber")
    private fun classifyStrength(coefficient: Double): CorrelationStrength {
        val abs = kotlin.math.abs(coefficient)
        return when {
            abs >= 0.7 -> CorrelationStrength.STRONG
            abs >= 0.5 -> CorrelationStrength.MODERATE
            else -> CorrelationStrength.WEAK
        }
    }
}
