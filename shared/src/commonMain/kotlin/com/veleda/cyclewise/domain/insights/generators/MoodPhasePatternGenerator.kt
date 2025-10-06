package com.veleda.cyclewise.domain.insights.generators

import com.veleda.cyclewise.domain.insights.Insight
import com.veleda.cyclewise.domain.insights.MoodPhasePattern
import com.veleda.cyclewise.domain.models.Cycle
import kotlinx.datetime.daysUntil
import kotlin.math.abs

private enum class MoodType { LOW, HIGH }

class MoodPhasePatternGenerator : InsightGenerator {

    override fun generate(data: InsightData): List<Insight> {
        val chronologicalCycles = data.allCycles.filter { it.endDate != null }.reversed()
        if (chronologicalCycles.size < 4) return emptyList()

        val cyclePairs = chronologicalCycles.zipWithNext()
        val logsByCycleId = data.allLogs.groupBy { it.entry.cycleId }

        val patternTally = mutableMapOf<Pair<MoodType, Int>, Int>()

        for ((currentCycle, nextCycle) in cyclePairs.takeLast(8)) {
            val actualCycleLength = currentCycle.startDate.daysUntil(nextCycle.startDate)
            val logsForCycle = logsByCycleId[currentCycle.id] ?: continue
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
     * Finds all dynamically-sized, non-overlapping, dense clusters of significant days.
     */
    private fun findAllSignificantBlocks(significantDays: List<Int>): List<List<Int>> {
        if (significantDays.isEmpty()) return emptyList()

        // 1. Group all significant days into blocks based on proximity (gap of <= 2 days).
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

        // A block is significant if it contains at least 3 individually significant days.
        return potentialBlocks.filter { it.size >= 3 }
    }

    private fun formatRelativePhase(group: List<Int>, cycles: List<Cycle>): String {
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
            val avgPeriodLength = cycles.mapNotNull { it.endDate?.let { endDate -> it.startDate.daysUntil(endDate) } }
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