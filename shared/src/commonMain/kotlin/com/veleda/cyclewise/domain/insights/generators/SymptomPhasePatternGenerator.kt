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

class SymptomPhasePatternGenerator : InsightGenerator {

    companion object {
        private const val HIGH_PRIORITY = 108 // For patterns during the period
        private const val NORMAL_PRIORITY = 98  // For patterns outside the period
    }

    @OptIn(ExperimentalTime::class)
    override fun generate(data: InsightData): List<Insight> {
        // Get all completed, logged periods, sorted from oldest to most recent.
        val chronologicalPeriods = data.allPeriods.filter { it.endDate != null }.reversed()

        if (chronologicalPeriods.size < 4) return emptyList()


        // This `cyclePairs` list is crucial. Each pair, like `(Period 1, Period 2)`, represents a single, complete menstrual cycle.
        // The length of this cycle is the time from the start of Period 1 to the start of Period 2.
        val cyclePairs = chronologicalPeriods.zipWithNext()

        val patternTally = mutableMapOf<Pair<String, Int>, Int>()


        // Iterate through each cycle (defined by the start of two consecutive periods).
        for ((currentPeriod, nextPeriod) in cyclePairs.takeLast(8)) {
            val actualCycleLength = currentPeriod.startDate.daysUntil(nextPeriod.startDate)

            // Find all logs that fall within the date range of this specific cycle.
            val logsForCycle = data.allLogs.filter {
                it.entry.entryDate >= currentPeriod.startDate && it.entry.entryDate < nextPeriod.startDate
            }
            if (logsForCycle.isEmpty()) continue

            for (log in logsForCycle) {
                for (symptomLog in log.symptomLogs) {
                    val day = log.entry.dayInCycle
                    // The normalization logic remains correct as dayInCycle is still calculated.
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

        // Calculate the user's average period length to help identify period-related symptoms.
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