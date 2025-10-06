package com.veleda.cyclewise.domain.insights.generators

import com.veleda.cyclewise.domain.insights.Insight
import com.veleda.cyclewise.domain.insights.NextPeriodPrediction
import com.veleda.cyclewise.domain.insights.SymptomPhasePattern
import com.veleda.cyclewise.domain.models.Cycle
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import kotlin.math.abs
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class SymptomPhasePatternGenerator : InsightGenerator {

    companion object {
        private const val HIGH_PRIORITY = 108 // For patterns during the period
        private const val NORMAL_PRIORITY = 98  // For patterns outside the period
    }

    @OptIn(ExperimentalTime::class)
    override fun generate(data: InsightData): List<Insight> {
        val chronologicalCycles = data.allCycles.filter { it.endDate != null }.reversed()
        if (chronologicalCycles.size < 4) return emptyList()

        val cyclePairs = chronologicalCycles.zipWithNext()
        val logsByCycleId = data.allLogs.groupBy { it.entry.cycleId }
        val periodDays = data.allCycles.flatMap { cycle ->
            cycle.endDate?.let { endDate ->
                (0..cycle.startDate.daysUntil(endDate)).map { cycle.startDate.plus(it, DateTimeUnit.DAY) }
            } ?: emptyList()
        }.toSet()

        val patternTally = mutableMapOf<Pair<String, Int>, Int>()
        for ((currentCycle, nextCycle) in cyclePairs.takeLast(8)) {
            val actualCycleLength = currentCycle.startDate.daysUntil(nextCycle.startDate)
            val logsForCycle = logsByCycleId[currentCycle.id] ?: continue
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

        for ((symptomId, normalizedDays) in patternsBySymptom) {
            val symptom = data.symptomLibrary.find { it.id == symptomId } ?: continue
            val sortedDays = normalizedDays.sorted()
            val groupedDays = groupConsecutiveDays(sortedDays)

            for (group in groupedDays) {
                val logsForPattern = data.allLogs.filter { log ->
                    val cycle = chronologicalCycles.find { it.id == log.entry.cycleId }
                    val nextCycle = chronologicalCycles.getOrNull(chronologicalCycles.indexOf(cycle) + 1)
                    val normalizedDay = if (cycle != null && nextCycle != null) {
                        val actualCycleLength = cycle.startDate.daysUntil(nextCycle.startDate)
                        if (log.entry.dayInCycle <= 16) log.entry.dayInCycle else log.entry.dayInCycle - actualCycleLength - 1
                    } else { log.entry.dayInCycle }
                    log.symptomLogs.any { it.symptomId == symptomId } && normalizedDay in group
                }
                val periodLogInCount = logsForPattern.count { it.entry.entryDate in periodDays }
                val isPeriodSymptom = logsForPattern.isNotEmpty() && (periodLogInCount.toDouble() / logsForPattern.size) > 0.5
                val priority = if (isPeriodSymptom) HIGH_PRIORITY else NORMAL_PRIORITY

                val recurrenceCount = group.mapNotNull { day -> significantPatterns[Pair(symptomId, day)] }.minOrNull() ?: 0
                val phaseDescription = formatPhaseDescription(group, isPeriodSymptom, chronologicalCycles)

                var finalPredictedDate: kotlinx.datetime.LocalDate? = null
                var chanceDescription: String? = null

                if (nextPeriodPrediction != null) {
                    val representativeDay = group.average().toInt()

                    val predictedDate = if (representativeDay < 0) {
                        nextPeriodPrediction.predictedDate.plus(representativeDay, DateTimeUnit.DAY)
                    } else {
                        val predictedCycleStart = nextPeriodPrediction.predictedDate.plus(-(data.averageCycleLength?.toInt() ?: 28), DateTimeUnit.DAY)
                        predictedCycleStart.plus(representativeDay -1, DateTimeUnit.DAY)
                    }

                    // Only include the prediction if the date is today or in the future.
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
     * The main formatter that chooses between "day of period" and the more complex "before/after" wording.
     */
    private fun formatPhaseDescription(group: List<Int>, isPeriodSymptom: Boolean, cycles: List<Cycle>): String {
        if (group.isEmpty()) return ""

        if (isPeriodSymptom) {
            // Use simple, user-friendly wording for period symptoms.
            return when (group.size) {
                1 -> "on day ${group.first()} of your period"
                2 -> "on days ${group.first()} & ${group.last()} of your period"
                else -> "on days ${group.first()}-${group.last()} of your period"
            }
        } else {
            // Use the advanced "before/after" logic for all other symptoms.
            return formatRelativePhase(group, cycles)
        }
    }

    /**
     * New, advanced formatter that describes a day relative to the start or end of a period.
     */
    private fun formatRelativePhase(group: List<Int>, cycles: List<Cycle>): String {
        if (group.isEmpty()) return ""

        // For a group, we use the average day to determine its position.
        val representativeDay = group.average().toInt()
        val isLutealPhase = representativeDay > 16 || representativeDay < 0 // Occurs in second half of cycle

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
            // Find the average period length to make the "after" more accurate.
            val avgPeriodLength = cycles.mapNotNull { it.endDate?.let { endDate -> it.startDate.daysUntil(endDate) } }.average().toInt()
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