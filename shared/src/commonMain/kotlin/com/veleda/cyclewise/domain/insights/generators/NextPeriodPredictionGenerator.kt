package com.veleda.cyclewise.domain.insights.generators

import com.veleda.cyclewise.domain.insights.Insight
import com.veleda.cyclewise.domain.insights.NextPeriodPrediction
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import kotlin.math.roundToInt
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Predicts the start date of the next menstrual period.
 *
 * Calculation: `latestPeriod.startDate + averageCycleLength (rounded)`.
 *
 * Returns an empty list if the average cycle length is unavailable (fewer than 2 completed
 * periods) or if no periods exist at all.
 *
 * This generator runs first in the [InsightEngine] pipeline so that other generators
 * (e.g., [SymptomPhasePatternGenerator]) can use its prediction for recurrence forecasting.
 */
class NextPeriodPredictionGenerator : InsightGenerator {
    @OptIn(ExperimentalTime::class)
    override fun generate(data: InsightData): List<Insight> {
        val latestCycle = data.allPeriods.firstOrNull()
        if (data.averageCycleLength == null || latestCycle == null) {
            return emptyList()
        }

        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val averageLengthInDays = data.averageCycleLength.roundToInt()
        val predictedDate = latestCycle.startDate.plus(averageLengthInDays, DateTimeUnit.DAY)

        val daysUntil = today.daysUntil(predictedDate)

        return listOf(NextPeriodPrediction(
            predictedDate = predictedDate,
            daysUntilPrediction = daysUntil
        ))
    }
}