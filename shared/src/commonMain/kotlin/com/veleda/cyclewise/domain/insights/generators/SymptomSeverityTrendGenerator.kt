package com.veleda.cyclewise.domain.insights.generators

import com.veleda.cyclewise.domain.insights.Insight
import com.veleda.cyclewise.domain.insights.SymptomSeverityTrend
import com.veleda.cyclewise.domain.insights.analysis.CycleAnalyzer

/**
 * Compares average symptom severity across recent and older cycles to detect trends.
 *
 * Splits analyzed cycles into an older half and a recent half, computes the average
 * severity for each half, and produces a [SymptomSeverityTrend] insight if the
 * difference exceeds a threshold.
 *
 * ## Minimum Data
 * Requires at least 3 completed periods (2 full cycles) with symptom log data.
 */
class SymptomSeverityTrendGenerator : InsightGenerator {

    companion object {
        private const val SIGNIFICANT_DIFFERENCE = 0.3
    }

    override fun generate(data: InsightData): List<Insight> {
        val boundaries = CycleAnalyzer.buildCycleBoundaries(data.allPeriods)
        if (boundaries.size < 2) return emptyList()

        val severitiesByCycle = boundaries.map { boundary ->
            val logsForCycle = data.allLogs.filter {
                it.entry.entryDate >= boundary.startDate && it.entry.entryDate < boundary.nextStartDate
            }
            logsForCycle.flatMap { log -> log.symptomLogs.map { it.severity } }
        }

        val nonEmptyCycles = severitiesByCycle.filter { it.isNotEmpty() }
        if (nonEmptyCycles.size < 2) return emptyList()

        val midpoint = nonEmptyCycles.size / 2
        val olderHalf = nonEmptyCycles.take(midpoint)
        val recentHalf = nonEmptyCycles.drop(midpoint)

        val avgOlder = olderHalf.flatten().average()
        val avgRecent = recentHalf.flatten().average()

        if (avgOlder.isNaN() || avgRecent.isNaN()) return emptyList()

        val difference = avgRecent - avgOlder
        val trendDescription = when {
            difference > SIGNIFICANT_DIFFERENCE -> "increasing"
            difference < -SIGNIFICANT_DIFFERENCE -> "decreasing"
            else -> "stable"
        }

        return listOf(
            SymptomSeverityTrend(
                trendDescription = trendDescription,
                avgSeverityRecent = avgRecent,
                avgSeverityOlder = avgOlder,
            )
        )
    }
}
