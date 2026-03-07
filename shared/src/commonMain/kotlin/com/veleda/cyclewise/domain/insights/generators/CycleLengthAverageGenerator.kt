package com.veleda.cyclewise.domain.insights.generators

import com.veleda.cyclewise.domain.insights.CycleLengthAverage
import com.veleda.cyclewise.domain.insights.Insight

/**
 * Passes through the pre-calculated average cycle length as a [CycleLengthAverage] insight.
 * Returns an empty list if the average is unavailable (fewer than 2 completed periods).
 */
class CycleLengthAverageGenerator : InsightGenerator {
    /**
     * Wraps the pre-calculated [InsightData.averageCycleLength] in a
     * [CycleLengthAverage] insight.
     *
     * @param data Aggregated cycle data; the average must be non-null (>= 2 completed periods).
     * @return A single-element list containing the average, or empty if unavailable.
     */
    override fun generate(data: InsightData): List<Insight> {
        return if (data.averageCycleLength != null) {
            listOf(CycleLengthAverage(data.averageCycleLength))
        } else {
            emptyList()
        }
    }
}