package com.veleda.cyclewise.domain.insights.generators

import com.veleda.cyclewise.domain.insights.Insight
import com.veleda.cyclewise.domain.insights.NextPeriodPrediction
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.plus
import kotlin.math.roundToInt

class NextPeriodPredictionGenerator : InsightGenerator {
    override fun generate(data: InsightData): List<Insight> {
        val latestCycle = data.allPeriods.firstOrNull()
        if (data.averageCycleLength == null || latestCycle == null) {
            return emptyList()
        }

        val averageLengthInDays = data.averageCycleLength.roundToInt()
        val predictedDate = latestCycle.startDate.plus(averageLengthInDays, DateTimeUnit.DAY)
        return listOf(NextPeriodPrediction(predictedDate))
    }
}