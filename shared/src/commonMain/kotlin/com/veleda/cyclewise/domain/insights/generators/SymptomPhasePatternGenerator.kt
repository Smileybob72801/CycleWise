package com.veleda.cyclewise.domain.insights.generators

import com.veleda.cyclewise.domain.insights.Insight
import com.veleda.cyclewise.domain.insights.NextPeriodPrediction
import com.veleda.cyclewise.domain.insights.SymptomPhasePattern
import com.veleda.cyclewise.domain.insights.analysis.CycleAnalyzer
import com.veleda.cyclewise.domain.insights.analysis.DataExtractors
import com.veleda.cyclewise.domain.insights.analysis.PhaseDescriptionFormatter
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Detects recurring symptom-to-cycle-phase correlations across recent cycles.
 *
 * ## Algorithm Overview
 *
 * 1. **Cycle pairing:** Consecutive completed periods are paired to define cycles.
 *    Each cycle spans from one period's start to the next period's start.
 *
 * 2. **Day normalization:** Each symptom occurrence's `dayInCycle` is normalized to a
 *    phase-relative offset. Days 1-16 are kept as-is (follicular phase). Days > 16 are
 *    converted to negative offsets from the next period start (`day - cycleLength - 1`),
 *    making luteal-phase patterns comparable across variable-length cycles.
 *
 * 3. **Pattern tallying:** Symptom occurrences are tallied per `(symptomId, normalizedDay)`
 *    across the last 8 cycles (via `takeLast(8)`).
 *
 * 4. **Significance filtering:** A pattern is significant if it occurs in >= 1 cycle
 *    AND in >= 60% of analyzed cycles.
 *
 * 5. **Grouping:** Significant days for each symptom are grouped into consecutive-day
 *    clusters. Each cluster produces one [SymptomPhasePattern] insight.
 *
 * 6. **Prediction:** If a [NextPeriodPrediction] is available, the generator estimates
 *    when the symptom will next recur and assigns a probability label.
 *
 * ## Priority
 * - Patterns during the period itself receive [HIGH_PRIORITY] (108).
 * - Patterns outside the period receive [NORMAL_PRIORITY] (98).
 *
 * ## Minimum Data
 * Requires at least 2 completed periods (1 full cycle).
 */
class SymptomPhasePatternGenerator : InsightGenerator {

    companion object {
        private const val HIGH_PRIORITY = 108
        private const val NORMAL_PRIORITY = 98
    }

    /**
     * Analyzes up to the last 8 cycles for recurring symptom-to-phase correlations.
     *
     * Normalizes each symptom occurrence's day-in-cycle to a phase-relative offset,
     * tallies occurrences per `(symptomId, normalizedDay)`, and filters for patterns
     * recurring in >= 60 % of cycles. Consecutive significant days are grouped, and
     * each group produces a [SymptomPhasePattern] insight with an optional recurrence
     * prediction when a [NextPeriodPrediction] is available.
     *
     * @param data Aggregated cycle data; requires >= 2 completed periods.
     * @return Zero or more [SymptomPhasePattern] insights, one per symptom cluster.
     */
    @OptIn(ExperimentalTime::class)
    override fun generate(data: InsightData): List<Insight> {
        val boundaries = CycleAnalyzer.buildCycleBoundaries(data.allPeriods)
        if (boundaries.isEmpty()) return emptyList()

        val tally = CycleAnalyzer.tallyPatterns(boundaries, data.allLogs, DataExtractors.symptoms)
        val significantPatterns = CycleAnalyzer.filterSignificant(tally, boundaries.size)
        if (significantPatterns.isEmpty()) return emptyList()

        val blocks = CycleAnalyzer.buildRecurrenceBlocks(
            significantPatterns = significantPatterns,
            totalCycles = boundaries.size,
            maxGap = 1,
            minBlockSize = 1,
        )

        val nextPeriodPrediction = data.generatedInsights
            .filterIsInstance<NextPeriodPrediction>().firstOrNull()

        val avgPeriodLength = data.allPeriods
            .mapNotNull { it.endDate?.let { endDate -> it.startDate.daysUntil(endDate) + 1 } }
            .average()
            .takeIf { !it.isNaN() } ?: 5.0

        val chronologicalPeriods = data.allPeriods.filter { it.endDate != null }.reversed()
        val resultingInsights = mutableListOf<SymptomPhasePattern>()

        for (block in blocks) {
            val symptom = data.symptomLibrary.find { it.id == block.dataId } ?: continue
            val isPeriodSymptom = PhaseDescriptionFormatter.isPeriodPhase(
                block.normalizedDays, avgPeriodLength
            )
            val priority = if (isPeriodSymptom) HIGH_PRIORITY else NORMAL_PRIORITY
            val phaseDescription = PhaseDescriptionFormatter.format(
                block.normalizedDays, isPeriodSymptom, chronologicalPeriods
            )

            var finalPredictedDate: kotlinx.datetime.LocalDate? = null
            var chanceDescription: String? = null

            if (nextPeriodPrediction != null) {
                val representativeDay = block.normalizedDays.average().toInt()
                val predictedDate = if (representativeDay < 0) {
                    nextPeriodPrediction.predictedDate.plus(representativeDay, DateTimeUnit.DAY)
                } else {
                    val predictedCycleStart = nextPeriodPrediction.predictedDate.plus(
                        -(data.averageCycleLength?.toInt() ?: 28), DateTimeUnit.DAY
                    )
                    predictedCycleStart.plus(representativeDay - 1, DateTimeUnit.DAY)
                }

                val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
                if (predictedDate >= today) {
                    finalPredictedDate = predictedDate
                    val probability = block.recurrenceCount.toDouble() / boundaries.size
                    chanceDescription = when {
                        probability >= 0.85 -> "a high chance"
                        probability >= 0.70 -> "a good chance"
                        else -> "a chance"
                    }
                }
            }

            resultingInsights.add(
                SymptomPhasePattern(
                    symptomName = symptom.name,
                    phaseDescription = phaseDescription,
                    recurrenceRate = "${block.recurrenceCount} out of ${boundaries.size}",
                    priority = priority,
                    predictedDate = finalPredictedDate,
                    chanceDescription = chanceDescription,
                )
            )
        }
        return resultingInsights
    }
}
