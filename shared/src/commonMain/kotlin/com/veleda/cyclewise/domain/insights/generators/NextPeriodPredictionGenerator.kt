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

        // Calculate the days until prediction
        val daysUntil = today.daysUntil(predictedDate)

        // Pass the raw date and the calculated daysUntil
        return listOf(NextPeriodPrediction(
            predictedDate = predictedDate,
            daysUntilPrediction = daysUntil
        ))
    }
}