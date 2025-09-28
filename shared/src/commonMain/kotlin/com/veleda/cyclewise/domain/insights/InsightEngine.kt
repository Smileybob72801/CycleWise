package com.veleda.cyclewise.domain.insights

import com.veleda.cyclewise.domain.models.Cycle
import com.veleda.cyclewise.domain.models.Symptom
import com.veleda.cyclewise.domain.models.SymptomLog
import kotlinx.datetime.daysUntil

/**
 * Analyzes user data to generate a list of actionable or interesting insights.
 */
class InsightEngine {

    /**
     * The primary function that orchestrates the generation of all available insights.
     *
     * @param allCycles A complete list of the user's cycles.
     * @param allSymptomLogs A complete list of all symptom log entries.
     * @param symptomLibrary The user's symptom library for name lookups.
     * @return A list of generated `Insight` objects, sorted by priority.
     */
    fun generateInsights(
        allCycles: List<Cycle>,
        allSymptomLogs: List<SymptomLog>,
        symptomLibrary: List<Symptom>
    ): List<Insight> {
        val insights = mutableListOf<Insight>()

        // --- Generator 1: Cycle Length Average ---
        generateCycleLengthAverage(allCycles)?.let { insights.add(it) }

        // --- Generator 2: Symptom Recurrence ---
        generateSymptomRecurrence(allSymptomLogs, symptomLibrary)?.let { insights.add(it) }

        // Return all found insights, with the highest priority ones first.
        return insights.sortedByDescending { it.priority }
    }

    /**
     * Calculates the average length of a user's completed menstrual cycles.
     * Requires at least two completed cycles to generate a meaningful average.
     */
    private fun generateCycleLengthAverage(allCycles: List<Cycle>): CycleLengthAverage? {
        val completedCycles = allCycles.filter { it.endDate != null }

        // We need at least two data points to calculate a meaningful average.
        if (completedCycles.size < 2) {
            return null
        }

        val average = completedCycles
            .map { it.startDate.daysUntil(it.endDate!!) + 1 } // +1 to be inclusive
            .average()

        return CycleLengthAverage(average)
    }

    /**
     * Identifies the symptom that has been logged most frequently by the user.
     */
    private fun generateSymptomRecurrence(
        allSymptomLogs: List<SymptomLog>,
        symptomLibrary: List<Symptom>
    ): SymptomRecurrence? {
        if (allSymptomLogs.isEmpty()) {
            return null
        }

        // Group logs by symptomId and find the group with the largest size.
        val mostFrequentEntry = allSymptomLogs
            .groupBy { it.symptomId }
            .maxByOrNull { it.value.size }

        if (mostFrequentEntry == null) {
            return null
        }

        // Look up the symptom's name from the library using its ID.
        val symptomInfo = symptomLibrary.find { it.id == mostFrequentEntry.key }

        return symptomInfo?.let {
            SymptomRecurrence(symptomName = it.name)
        }
    }
}