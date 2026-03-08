package com.veleda.cyclewise.domain.insights.generators

import com.veleda.cyclewise.domain.insights.CycleSummary
import com.veleda.cyclewise.domain.insights.Insight
import com.veleda.cyclewise.domain.insights.NextPeriodPrediction
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.todayIn
import kotlin.math.roundToInt
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Generates a high-level summary of the current or most recent cycle.
 *
 * Produces a [CycleSummary] insight that shows the current cycle day, estimated
 * cycle length, phase name, and days until next period (if prediction available).
 *
 * ## Minimum Data
 * Requires at least 1 period (ongoing or completed).
 */
class CycleSummaryGenerator : InsightGenerator {

    @OptIn(ExperimentalTime::class)
    override fun generate(data: InsightData): List<Insight> {
        val latestPeriod = data.allPeriods.firstOrNull() ?: return emptyList()

        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val cycleDay = latestPeriod.startDate.daysUntil(today) + 1

        if (cycleDay < 1) return emptyList()

        val cycleLength = data.averageCycleLength?.roundToInt() ?: 28

        val phaseName = when {
            cycleDay <= 5 -> "menstruation"
            cycleDay <= cycleLength - 14 -> "follicular"
            cycleDay <= cycleLength - 11 -> "ovulation"
            else -> "luteal"
        }

        val daysUntilNextPeriod = data.generatedInsights
            .filterIsInstance<NextPeriodPrediction>()
            .firstOrNull()
            ?.daysUntilPrediction

        return listOf(
            CycleSummary(
                cycleDay = cycleDay,
                cycleLength = cycleLength,
                phaseName = phaseName,
                daysUntilNextPeriod = daysUntilNextPeriod,
            )
        )
    }
}
