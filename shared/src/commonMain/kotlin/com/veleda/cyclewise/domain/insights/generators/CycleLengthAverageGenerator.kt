package com.veleda.cyclewise.domain.insights.generators

import com.veleda.cyclewise.domain.insights.CycleLengthAverage
import com.veleda.cyclewise.domain.insights.Insight

class CycleLengthAverageGenerator : InsightGenerator {
    override fun generate(data: InsightData): List<Insight> {
        return if (data.averageCycleLength != null) {
            listOf(CycleLengthAverage(data.averageCycleLength))
        } else {
            emptyList()
        }
    }
}