package com.veleda.cyclewise.domain.insights

import com.veleda.cyclewise.domain.models.Cycle
import com.veleda.cyclewise.domain.models.Symptom
import com.veleda.cyclewise.domain.models.SymptomLog
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil
import kotlinx.datetime.plus
import kotlin.math.roundToInt

/**
 * Analyzes user data to generate a list of actionable or interesting insights.
 */
class InsightEngine {

    /**
     * The primary function that orchestrates the generation of all available insights.
     */
    fun generateInsights(
        allCycles: List<Cycle>,
        allSymptomLogs: List<SymptomLog>,
        symptomLibrary: List<Symptom>
    ): List<Insight> {
        val insights = mutableListOf<Insight>()

        // Calculate the correct average cycle length once.
        val averageCycleLength = calculateAverageCycleLength(allCycles)

        // --- Generator 1: Next Period Prediction ---
        // This generator uses the calculated average.
        if (averageCycleLength != null) {
            generateNextPeriodPrediction(allCycles, averageCycleLength)?.let { insights.add(it) }
        }

        // --- Generator 2: Cycle Length Average ---
        // This generator also uses the calculated average.
        if (averageCycleLength != null) {
            insights.add(CycleLengthAverage(averageCycleLength))
        }

        // --- Generator 3: Symptom Recurrence ---
        generateSymptomRecurrence(allSymptomLogs, symptomLibrary)?.let { insights.add(it) }

        return insights.sortedByDescending { it.priority }
    }

    private fun calculateAverageCycleLength(allCycles: List<Cycle>): Double? {
        val completedCycles = allCycles.filter { it.endDate != null }

        // The repo sorts descending, so we reverse to get chronological order.
        val chronologicalCycles = completedCycles.reversed()

        // We need at least two cycles to calculate one duration between them.
        if (chronologicalCycles.size < 2) {
            return null
        }

        // Use zipWithNext to create pairs of consecutive cycles, e.g., (Cycle1, Cycle2), (Cycle2, Cycle3).
        val cycleDurations = chronologicalCycles.zipWithNext { current, next ->
            current.startDate.daysUntil(next.startDate).toDouble()
        }

        // It's possible to have cycles but no durations if there are fewer than 2.
        if (cycleDurations.isEmpty()) return null

        return cycleDurations.average()
    }

    private fun generateNextPeriodPrediction(allCycles: List<Cycle>, averageLength: Double): NextPeriodPrediction? {
        // The repository sorts cycles by start date descending, so the first is the latest.
        val latestCycle = allCycles.firstOrNull() ?: return null

        val averageLengthInDays = averageLength.roundToInt()

        // The prediction is the start date of the last cycle plus the average length.
        val predictedDate = latestCycle.startDate.plus(averageLengthInDays, DateTimeUnit.DAY)

        return NextPeriodPrediction(predictedDate)
    }

    private fun generateSymptomRecurrence(
        allSymptomLogs: List<SymptomLog>,
        symptomLibrary: List<Symptom>
    ): SymptomRecurrence? {
        if (allSymptomLogs.isEmpty()) {
            return null
        }
        val mostFrequentEntry = allSymptomLogs
            .groupBy { it.symptomId }
            .maxByOrNull { it.value.size }
            ?: return null

        val symptomInfo = symptomLibrary.find { it.id == mostFrequentEntry.key }

        return symptomInfo?.let {
            SymptomRecurrence(symptomName = it.name)
        }
    }
}