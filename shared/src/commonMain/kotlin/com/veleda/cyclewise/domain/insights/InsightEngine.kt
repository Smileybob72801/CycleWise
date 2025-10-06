package com.veleda.cyclewise.domain.insights

import com.veleda.cyclewise.domain.insights.generators.InsightData
import com.veleda.cyclewise.domain.insights.generators.InsightGenerator
import com.veleda.cyclewise.domain.insights.generators.NextPeriodPredictionGenerator
import com.veleda.cyclewise.domain.insights.generators.SymptomPhasePatternGenerator
import com.veleda.cyclewise.domain.models.Cycle
import com.veleda.cyclewise.domain.models.FullDailyLog
import com.veleda.cyclewise.domain.models.Symptom
import kotlinx.datetime.daysUntil

class InsightEngine(
    private val generators: List<InsightGenerator>
) {
    fun generateInsights(
        allCycles: List<Cycle>,
        allLogs: List<FullDailyLog>,
        symptomLibrary: List<Symptom>
    ): List<Insight> {
        val insights = mutableListOf<Insight>()

        // 1. Prepare the initial data.
        val baseData = InsightData(
            allCycles = allCycles,
            allLogs = allLogs,
            symptomLibrary = symptomLibrary,
            averageCycleLength = calculateAverageCycleLength(allCycles)
        )

        // 2. Run generators that have no dependencies first (like prediction).
        // This is a simple way to handle dependency order.
        val predictionGenerator = generators.find { it is NextPeriodPredictionGenerator }
        predictionGenerator?.let { insights.addAll(it.generate(baseData)) }

        // 3. Prepare data for the next set of generators, now including the prediction.
        val dataWithPrediction = baseData.copy(generatedInsights = insights)

        // 4. Run all other generators.
        generators.filterNot { it is NextPeriodPredictionGenerator }.forEach { generator ->
            insights.addAll(generator.generate(dataWithPrediction))
        }

        // 5. Sort the final list by priority.
        return insights.distinctBy { it.id }.sortedByDescending { it.priority }
    }

    private fun calculateAverageCycleLength(allCycles: List<Cycle>): Double? {
        val chronologicalCycles = allCycles.filter { it.endDate != null }.reversed()
        if (chronologicalCycles.size < 2) return null
        val cycleDurations = chronologicalCycles.zipWithNext { current, next ->
            current.startDate.daysUntil(next.startDate).toDouble()
        }
        return cycleDurations.takeIf { it.isNotEmpty() }?.average()
    }
}