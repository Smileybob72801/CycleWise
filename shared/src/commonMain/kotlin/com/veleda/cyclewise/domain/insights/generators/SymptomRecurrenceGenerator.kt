package com.veleda.cyclewise.domain.insights.generators

import com.veleda.cyclewise.domain.insights.Insight
import com.veleda.cyclewise.domain.insights.TopSymptomsInsight

class SymptomRecurrenceGenerator : InsightGenerator {
    override fun generate(data: InsightData): List<Insight> {
        val allSymptomLogs = data.allLogs.flatMap { it.symptomLogs }
        if (allSymptomLogs.size < 10) { // Require at least 10 logs to be meaningful
            return emptyList()
        }

        // 1. Get the top X symptom IDs
        val frequentSymptomIds = allSymptomLogs
            .groupingBy { it.symptomId }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
            .take(data.topSymptomsCount)
            .map { it.key }

        // 2. Map the IDs to their names.
        val topSymptomNames = frequentSymptomIds.mapNotNull { symptomId ->
            data.symptomLibrary.find { it.id == symptomId }?.name
        }

        // 3. If we have any results, create a single insight object containing the list.
        return if (topSymptomNames.isNotEmpty()) {
            listOf(TopSymptomsInsight(topSymptomNames))
        } else {
            emptyList()
        }
    }
}