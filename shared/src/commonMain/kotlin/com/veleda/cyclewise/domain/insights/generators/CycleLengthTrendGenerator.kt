package com.veleda.cyclewise.domain.insights.generators

import com.veleda.cyclewise.domain.insights.CycleLengthTrend
import com.veleda.cyclewise.domain.insights.Insight
import kotlinx.datetime.daysUntil
import kotlin.math.abs
import kotlin.math.roundToInt

class CycleLengthTrendGenerator : InsightGenerator {
    override fun generate(data: InsightData): List<Insight> {
        val completedCycles = data.allPeriods.filter { it.endDate != null }.reversed()
        if (completedCycles.size < 6) {
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