package com.veleda.cyclewise.domain.insights.generators

import com.veleda.cyclewise.domain.insights.Insight
import com.veleda.cyclewise.domain.insights.TopSymptomsInsight

/**
 * Identifies the user's most frequently logged symptoms across all time.
 *
 * Returns a single [TopSymptomsInsight] containing the top N symptom names by occurrence count,
 * where N is configured by [InsightData.topSymptomsCount].
 *
 * ## Minimum Data
 * Requires at least 3 total symptom log entries to produce a result.
 */
class SymptomRecurrenceGenerator : InsightGenerator {
    override fun generate(data: InsightData): List<Insight> {
        val allSymptomLogs = data.allLogs.flatMap { it.symptomLogs }
        if (allSymptomLogs.size < 3) {
            return emptyList()
        }

        val frequentSymptomIds = allSymptomLogs
            .groupingBy { it.symptomId }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
            .take(data.topSymptomsCount)
            .map { it.key }

        val topSymptomNames = frequentSymptomIds.mapNotNull { symptomId ->
            data.symptomLibrary.find { it.id == symptomId }?.name
        }

        return if (topSymptomNames.isNotEmpty()) {
            listOf(TopSymptomsInsight(topSymptomNames))
        } else {
            emptyList()
        }
    }
}