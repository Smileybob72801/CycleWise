package com.veleda.cyclewise.domain.insights.generators

import com.veleda.cyclewise.domain.insights.CycleLengthTrend
import com.veleda.cyclewise.domain.insights.Insight
import kotlinx.datetime.daysUntil
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Detects whether cycle length is trending longer, shorter, or remaining stable.
 *
 * Splits the user's cycle duration history into an older half and a recent half,
 * compares their averages, and reports the trend.
 *
 * ## Minimum Data
 * Requires at least 3 completed periods (2 inter-cycle durations).
 *
 * ## Threshold
 * A difference of > 1 day between halves is considered a meaningful trend.
 * Differences <= 1 day are reported as "remained consistent".
 */
class CycleLengthTrendGenerator : InsightGenerator {
    /**
     * Splits completed cycle durations into an older and a recent half, compares
     * their averages, and emits a [CycleLengthTrend] indicating whether the
     * cycle length has lengthened, shortened, or remained consistent.
     *
     * A difference of > 1 day is considered a meaningful trend.
     *
     * @param data Aggregated cycle data; requires >= 3 completed periods.
     * @return A single-element list with the trend, or empty if insufficient data.
     */
    override fun generate(data: InsightData): List<Insight> {
        val completedCycles = data.allPeriods.filter { it.endDate != null }.reversed()
        if (completedCycles.size < 3) {
            return emptyList()
        }

        val durations = completedCycles.zipWithNext { current, next -> current.startDate.daysUntil(next.startDate) }

        val recentHalf = durations.takeLast(durations.size / 2)
        val olderHalf = durations.take(durations.size / 2)

        if (recentHalf.isEmpty() || olderHalf.isEmpty()) {
            return emptyList()
        }

        val recentAverage = recentHalf.average()
        val olderAverage = olderHalf.average()
        val difference = (recentAverage - olderAverage).roundToInt()

        val trendInsight = when {
            abs(difference) > 1 -> {
                val trend = if (difference > 0) "lengthened" else "shortened"
                CycleLengthTrend(trend, abs(difference))
            }
            else -> CycleLengthTrend("remained consistent", 0)
        }

        return listOf(trendInsight)
    }
}