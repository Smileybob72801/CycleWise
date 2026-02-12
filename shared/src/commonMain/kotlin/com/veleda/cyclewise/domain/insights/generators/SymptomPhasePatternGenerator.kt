package com.veleda.cyclewise.domain.insights.generators

import com.veleda.cyclewise.domain.insights.Insight
import com.veleda.cyclewise.domain.insights.NextPeriodPrediction
import com.veleda.cyclewise.domain.insights.SymptomPhasePattern
import com.veleda.cyclewise.domain.models.Period
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import kotlin.math.abs
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
 * 4. **Significance filtering:** A pattern is significant if it occurs in >= 3 cycles
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
 * Requires at least 4 completed periods (3 full cycles).
 */
class SymptomPhasePatternGenerator : InsightGenerator {

    companion object {
        private const val HIGH_PRIORITY = 108
        private const val NORMAL_PRIORITY = 98
    }

    @OptIn(ExperimentalTime::class)
    override fun generate(data: InsightData): List<Insight> {
        val chronologicalPeriods = data.allPeriods.filter { it.endDate != null }.reversed()

        if (chronologicalPeriods.size < 4) return emptyList()

        val cyclePairs = chronologicalPeriods.zipWithNext()

        val patternTally = mutableMapOf<Pair<String, Int>, Int>()

        // Analyze the most recent 8 cycles.
        for ((currentPeriod, nextPeriod) in cyclePairs.takeLast(8)) {
            val actualCycleLength = currentPeriod.startDate.daysUntil(nextPeriod.startDate)

            val logsForCycle = data.allLogs.filter {
                it.entry.entryDate >= currentPeriod.startDate && it.entry.entryDate < nextPeriod.startDate
            }
            if (logsForCycle.isEmpty()) continue

            for (log in logsForCycle) {
                for (symptomLog in log.symptomLogs) {
                    val day = log.entry.dayInCycle
                    val normalizedDay = if (day <= 16) day else day - actualCycleLength - 1
                    val key = Pair(symptomLog.symptomId, normalizedDay)
                    patternTally[key] = (patternTally[key] ?: 0) + 1
                }
            }
        }

        val significantPatterns = patternTally.filter { (_, count) ->
            count >= 3 && (count.toDouble() / cyclePairs.size) >= 0.60
        }
        if (significantPatterns.isEmpty()) return emptyList()

        val nextPeriodPrediction = data.generatedInsights.filterIsInstance<NextPeriodPrediction>().firstOrNull()
        val patternsBySymptom = significantPatterns.entries.groupBy({ it.key.first }, { it.key.second })
        val resultingInsights = mutableListOf<SymptomPhasePattern>()

        val avgPeriodLength = data.allPeriods
            .mapNotNull { it.endDate?.let { endDate -> it.startDate.daysUntil(endDate) + 1 } }
            .average()
            .takeIf { !it.isNaN() } ?: 5.0 // Default to 5 days if not enough data.

        for ((symptomId, normalizedDays) in patternsBySymptom) {
            val symptom = data.symptomLibrary.find { it.id == symptomId } ?: continue
            val sortedDays = normalizedDays.distinct().sorted()
            val groupedDays = groupConsecutiveDays(sortedDays)

            for (group in groupedDays) {
                // A pattern is considered a "period symptom" if all its normalized days
                // fall within the calculated average period length.
                val isPeriodSymptom = group.all { it > 0 && it <= avgPeriodLength }
                val priority = if (isPeriodSymptom) HIGH_PRIORITY else NORMAL_PRIORITY

                val recurrenceCount = group.mapNotNull { day -> significantPatterns[Pair(symptomId, day)] }.minOrNull() ?: 0
                val phaseDescription = formatPhaseDescription(group, isPeriodSymptom, chronologicalPeriods)

                var finalPredictedDate: kotlinx.datetime.LocalDate? = null
                var chanceDescription: String? = null

                if (nextPeriodPrediction != null) {
                    val representativeDay = group.average().toInt()
                    val predictedDate = if (representativeDay < 0) {
                        nextPeriodPrediction.predictedDate.plus(representativeDay, DateTimeUnit.DAY)
                    } else {
                        val predictedCycleStart = nextPeriodPrediction.predictedDate.plus(-(data.averageCycleLength?.toInt() ?: 28), DateTimeUnit.DAY)
                        predictedCycleStart.plus(representativeDay - 1, DateTimeUnit.DAY)
                    }

                    val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
                    if (predictedDate >= today) {
                        finalPredictedDate = predictedDate
                        val probability = recurrenceCount.toDouble() / cyclePairs.size
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
                        recurrenceRate = "$recurrenceCount out of ${cyclePairs.size}",
                        priority = priority,
                        predictedDate = finalPredictedDate,
                        chanceDescription = chanceDescription
                    )
                )
            }
        }
        return resultingInsights
    }

    /**
     * Groups a sorted list of normalized day offsets into runs of consecutive integers.
     *
     * For example, `[1, 2, 3, 7, 8]` becomes `[[1, 2, 3], [7, 8]]`.
     * Each group represents a contiguous symptom cluster within a cycle phase.
     */
    private fun groupConsecutiveDays(days: List<Int>): List<List<Int>> {
        if (days.isEmpty()) return emptyList()
        val groups = mutableListOf<MutableList<Int>>()
        groups.add(mutableListOf(days.first()))
        for (i in 1 until days.size) {
            if (days[i] == days[i - 1] + 1) groups.last().add(days[i])
            else groups.add(mutableListOf(days[i]))
        }
        return groups
    }

    /**
     * Formats a human-readable phase description for a group of normalized days.
     *
     * Period symptoms (all days within average period length) get "on day X of your period".
     * Non-period symptoms are delegated to [formatRelativePhase] for "X days before/after" phrasing.
     */
    private fun formatPhaseDescription(group: List<Int>, isPeriodSymptom: Boolean, periods: List<Period>): String {
        if (group.isEmpty()) return ""

        if (isPeriodSymptom) {
            return when (group.size) {
                1 -> "on day ${group.first()} of your period"
                2 -> "on days ${group.first()} & ${group.last()} of your period"
                else -> "on days ${group.first()}-${group.last()} of your period"
            }
        } else {
            return formatRelativePhase(group, periods)
        }
    }

    /**
     * Formats a relative phase description for non-period symptom groups.
     *
     * Uses the representative day (group average) to determine the phase:
     * - **Luteal phase** (representativeDay > 16 or < 0): "X days before your period"
     * - **Follicular phase** (representativeDay <= 16): "X days after your period"
     *
     * The day-16 boundary approximates the follicular/luteal split in a typical cycle.
     */
    private fun formatRelativePhase(group: List<Int>, periods: List<Period>): String {
        if (group.isEmpty()) return ""
        val representativeDay = group.average().toInt()
        val isLutealPhase = representativeDay > 16 || representativeDay < 0

        if (isLutealPhase) {
            val daysBefore = abs(representativeDay)
            return when {
                daysBefore == 1 -> "on the day before your period"
                group.size == 1 -> "$daysBefore days before your period"
                else -> {
                    val firstDay = abs(group.last())
                    val lastDay = abs(group.first())
                    "from $firstDay to $lastDay days before your period"
                }
            }
        } else {
            val avgPeriodLength = periods.mapNotNull { it.endDate?.let { endDate -> it.startDate.daysUntil(endDate) } }.average().toInt()
            val daysAfter = representativeDay - avgPeriodLength
            return when {
                daysAfter <= 1 -> "right after your period"
                group.size == 1 -> "$daysAfter days after your period"
                else -> {
                    val firstDay = group.first() - avgPeriodLength
                    val lastDay = group.last() - avgPeriodLength
                    "from $firstDay to $lastDay days after your period"
                }
            }
        }
    }
}