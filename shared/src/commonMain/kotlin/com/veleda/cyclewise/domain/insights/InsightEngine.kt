package com.veleda.cyclewise.domain.insights

import com.veleda.cyclewise.domain.insights.generators.InsightData
import com.veleda.cyclewise.domain.insights.generators.InsightGenerator
import com.veleda.cyclewise.domain.insights.generators.NextPeriodPredictionGenerator
import com.veleda.cyclewise.domain.models.FullDailyLog
import com.veleda.cyclewise.domain.models.Medication
import com.veleda.cyclewise.domain.models.Period
import com.veleda.cyclewise.domain.models.Symptom
import com.veleda.cyclewise.domain.models.WaterIntake
import kotlinx.datetime.daysUntil

/**
 * Orchestrates insight generation by running registered [InsightGenerator]s in dependency order.
 *
 * Execution proceeds in two phases:
 * 1. [NextPeriodPredictionGenerator] runs first (other generators may depend on its output).
 * 2. All remaining generators run with the prediction result available in [InsightData.generatedInsights].
 *
 * Results are deduplicated by [Insight.id], scored by [InsightScorer], and returned
 * as [ScoredInsight]s sorted by relevance descending.
 */
class InsightEngine(
    private val generators: List<InsightGenerator>,
    private val scorer: InsightScorer = InsightScorer(),
) {
    /**
     * Generates all insights from the user's data, scored and sorted by relevance.
     *
     * @param allPeriods         All periods, sorted by start date descending.
     * @param allLogs            All daily logs across all dates.
     * @param symptomLibrary     The user's full symptom library (for ID-to-name lookups).
     * @param topSymptomsCount   How many top symptoms to include in [TopSymptomsInsight].
     * @param waterIntakes       All water intake records.
     * @param medicationLibrary  The user's medication library.
     * @return Scored insights sorted by relevance descending.
     */
    fun generateInsights(
        allPeriods: List<Period>,
        allLogs: List<FullDailyLog>,
        symptomLibrary: List<Symptom>,
        topSymptomsCount: Int,
        waterIntakes: List<WaterIntake> = emptyList(),
        medicationLibrary: List<Medication> = emptyList(),
    ): List<ScoredInsight> {
        val insights = mutableListOf<Insight>()

        val baseData = InsightData(
            allPeriods = allPeriods,
            allLogs = allLogs,
            symptomLibrary = symptomLibrary,
            averageCycleLength = calculateAverageCycleLength(allPeriods),
            topSymptomsCount = topSymptomsCount,
            waterIntakes = waterIntakes,
            medicationLibrary = medicationLibrary,
        )

        val predictionGenerator = generators.find { it is NextPeriodPredictionGenerator }
        predictionGenerator?.let { insights.addAll(it.generate(baseData)) }

        val dataWithPrediction = baseData.copy(generatedInsights = insights)
        generators.filterNot { it is NextPeriodPredictionGenerator }.forEach { generator ->
            insights.addAll(generator.generate(dataWithPrediction))
        }

        val deduplicated = insights.distinctBy { it.id }
        return scorer.score(deduplicated)
    }

    /**
     * Calculates the average cycle length from completed periods.
     *
     * Cycle length is defined as the number of days between consecutive period start dates
     * (start-to-start). Requires at least 2 completed periods to produce a result.
     *
     * @return the average cycle length in days, or null if fewer than 2 completed periods exist.
     */
    private fun calculateAverageCycleLength(allPeriods: List<Period>): Double? {
        val chronologicalCycles = allPeriods.filter { it.endDate != null }.reversed()
        if (chronologicalCycles.size < 2) return null
        val cycleDurations = chronologicalCycles.zipWithNext { current, next ->
            current.startDate.daysUntil(next.startDate).toDouble()
        }
        return cycleDurations.takeIf { it.isNotEmpty() }?.average()
    }
}
