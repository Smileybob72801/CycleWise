package com.veleda.cyclewise.domain.insights.generators

import com.veleda.cyclewise.domain.models.Cycle
import com.veleda.cyclewise.domain.models.FullDailyLog
import com.veleda.cyclewise.domain.models.Symptom
import com.veleda.cyclewise.domain.insights.Insight

/**
 * A container for all the data an insight generator might need.
 * This simplifies the generator interface.
 */
data class InsightData(
    val allCycles: List<Cycle>,
    val allLogs: List<FullDailyLog>,
    val symptomLibrary: List<Symptom>,
    val averageCycleLength: Double?,
    val generatedInsights: List<Insight> = emptyList()
)

/**
 * The contract for any class that can generate insights.
 */
interface InsightGenerator {
    fun generate(data: InsightData): List<Insight>
}