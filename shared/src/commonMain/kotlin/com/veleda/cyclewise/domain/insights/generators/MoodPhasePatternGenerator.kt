package com.veleda.cyclewise.domain.insights.generators

import com.veleda.cyclewise.domain.insights.Insight
import com.veleda.cyclewise.domain.insights.MoodPhasePattern
import com.veleda.cyclewise.domain.models.Period
import kotlinx.datetime.daysUntil
import kotlin.math.abs

private enum class MoodType { LOW, HIGH }

/**
 * Detects recurring mood-to-cycle-phase correlations across recent cycles.
 *
 * Uses the same day-normalization algorithm as [SymptomPhasePatternGenerator]:
 * days 1-16 are follicular-phase offsets; days > 16 become negative luteal-phase offsets.
 *
 * ## Mood Thresholds
 * - **LOW:** moodScore <= 2
 * - **HIGH:** moodScore >= 4
 * - Scores of 3 are excluded (neutral).
 *
 * ## Significance Criteria
 * Same as symptom patterns: >= 3 occurrences AND >= 60% recurrence across analyzed cycles.
 *
 * ## Block Detection
 * Significant days are grouped by proximity (gap <= 2 days). A block must contain
 * at least 3 individually significant days to produce an insight.
 *
 * ## Minimum Data
 * Requires at least 4 completed periods (3 full cycles).
 */
class MoodPhasePatternGenerator : InsightGenerator {

    override fun generate(data: InsightData): List<Insight> {
        val chronologicalCycles = data.allPeriods.filter { it.endDate != null }.reversed()
        if (chronologicalCycles.size < 4) return emptyList()

        val cyclePairs = chronologicalCycles.zipWithNext()

        val patternTally = mutableMapOf<Pair<MoodType, Int>, Int>()

        for ((currentCycle, nextCycle) in cyclePairs.takeLast(8)) {
            val actualCycleLength = currentCycle.startDate.daysUntil(nextCycle.startDate)

            val logsForCycle = data.allLogs.filter { it.entry.entryDate >= currentCycle.startDate
                    && it.entry.entryDate < nextCycle.startDate }

            if (logsForCycle.isEmpty()) continue

            for (log in logsForCycle) {
                val mood = log.entry.moodScore ?: continue
                val moodType = when {
                    mood <= 2 -> MoodType.LOW
                    mood >= 4 -> MoodType.HIGH
                    else -> null
                } ?: continue

                val day = log.entry.dayInCycle
                val normalizedDay = if (day <= 16) day else day - actualCycleLength - 1

                val key = Pair(moodType, normalizedDay)
                patternTally[key] = (patternTally[key] ?: 0) + 1
            }
        }

        val significantDays = patternTally.filter { (_, count) ->
            count >= 3 && (count.toDouble() / cyclePairs.size) >= 0.60
        }.map { it.key }

        if (significantDays.isEmpty()) return emptyList()

        val resultingInsights = mutableListOf<MoodPhasePattern>()

        for (moodType in MoodType.entries) {
            val daysForMood = significantDays.filter { it.first == moodType }.map { it.second }.sorted()
            if (daysForMood.isEmpty()) continue

            val allSignificantBlocks = findAllSignificantBlocks(daysForMood)

            for (block in allSignificantBlocks) {
                val recurrenceCount = block.mapNotNull { day -> patternTally[Pair(moodType, day)] }.minOrNull() ?: 0

               resultingInsights.add(
                    MoodPhasePattern(
                        moodType = moodType.name.lowercase(),
                        phaseDescription = formatRelativePhase(block, chronologicalCycles),
                        recurrenceRate = "$recurrenceCount out of ${cyclePairs.size}"
                    )
                )
            }
        }

        return resultingInsights
    }

    /**
     * Groups significant days into dense, non-overlapping clusters by proximity.
     *
     * Days within a gap of <= 2 are merged into the same block. A block is retained
     * only if it contains at least 3 individually significant days.
     *
     * @param significantDays sorted list of normalized day offsets that passed significance filtering.
     * @return non-overlapping blocks, each containing >= 3 consecutive-ish significant days.
     */
    private fun findAllSignificantBlocks(significantDays: List<Int>): List<List<Int>> {
        if (significantDays.isEmpty()) return emptyList()

        val potentialBlocks = mutableListOf<List<Int>>()
        var currentBlock = mutableListOf<Int>()
        for (day in significantDays) {
            if (currentBlock.isEmpty() || day - currentBlock.last() <= 2) {
                currentBlock.add(day)
            } else {
                potentialBlocks.add(currentBlock)
                currentBlock = mutableListOf(day)
            }
        }
        if (currentBlock.isNotEmpty()) potentialBlocks.add(currentBlock)

        return potentialBlocks.filter { it.size >= 3 }
    }

    /**
     * Formats a human-readable phase description for a mood pattern group.
     * Same phase boundary logic as [SymptomPhasePatternGenerator.formatRelativePhase].
     */
    private fun formatRelativePhase(group: List<Int>, periods: List<Period>): String {
        if (group.isEmpty()) return ""

        val representativeDay = group.average().toInt()
        val isLutealPhase = representativeDay > 16 || representativeDay < 0

        if (isLutealPhase) {
            val firstDayBefore = abs(group.last())
            val lastDayBefore = abs(group.first())
            return when {
                lastDayBefore == 1 && group.size == 1 -> "on the day before your period"
                group.size == 1 -> "$lastDayBefore days before your period"
                else -> "from $firstDayBefore to $lastDayBefore days before your period"
            }
        } else {
            val avgPeriodLength = periods.mapNotNull { it.endDate?.let { endDate -> it.startDate.daysUntil(endDate) } }
                .average().takeIf { !it.isNaN() }?.toInt() ?: 5

            val firstDayAfter = group.first() - avgPeriodLength
            val lastDayAfter = group.last() - avgPeriodLength

            return when {
                firstDayAfter <= 1 && lastDayAfter <= 1 -> "right after your period"
                group.size == 1 -> "$firstDayAfter days after your period"
                else -> "from $firstDayAfter to $lastDayAfter days after your period"
            }
        }
    }
}