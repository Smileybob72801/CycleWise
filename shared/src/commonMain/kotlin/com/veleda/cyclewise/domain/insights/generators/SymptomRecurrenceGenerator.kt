package com.veleda.cyclewise.domain.insights.generators

import com.veleda.cyclewise.domain.insights.Insight
import com.veleda.cyclewise.domain.insights.SymptomRecurrence

class SymptomRecurrenceGenerator : InsightGenerator {
    override fun generate(data: InsightData): List<Insight> {
        val allSymptomLogs = data.allLogs.flatMap { it.symptomLogs }
        if (allSymptomLogs.isEmpty()) {
            return emptyList()
        }

        val mostFrequentEntry = allSymptomLogs
            .groupBy { it.symptomId }
            .maxByOrNull { it.value.size }
            ?: return emptyList()

        val symptomInfo = data.symptomLibrary.find { it.id == mostFrequentEntry.key }

        return symptomInfo?.let {
            listOf(SymptomRecurrence(symptomName = it.name))
        } ?: emptyList()
    }
}