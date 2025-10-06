package com.veleda.cyclewise.domain.insights.generators

import com.veleda.cyclewise.domain.insights.Insight
import com.veleda.cyclewise.domain.insights.PremenstrualMoodPattern
import kotlinx.datetime.daysUntil

class PremenstrualMoodPatternGenerator : InsightGenerator {
    override fun generate(data: InsightData): List<Insight> {
        val chronologicalCycles = data.allCycles.filter { it.endDate != null }.reversed()
        if (chronologicalCycles.size < 4) {
            return emptyList()
        }

        val cyclePairs = chronologicalCycles.zipWithNext()
        val logsByCycleId = data.allLogs.groupBy { it.entry.cycleId }

        var cyclesWithLowPremenstrualMood = 0

        // Analyze up to the last 5 completed cycles
        for ((currentCycle, nextCycle) in cyclePairs.takeLast(5)) {
            val actualCycleLength = currentCycle.startDate.daysUntil(nextCycle.startDate)
            if (actualCycleLength <= 0) continue

            val premenstrualWindowStart = actualCycleLength - 3
            val logsForThisCycle = logsByCycleId[currentCycle.id] ?: continue

            val hadLowMood = logsForThisCycle.any { log ->
                val day = log.entry.dayInCycle
                val mood = log.entry.moodScore
                day >= premenstrualWindowStart && mood != null && mood < 3
            }

            if (hadLowMood) {
                cyclesWithLowPremenstrualMood++
            }
        }

        return if (cyclesWithLowPremenstrualMood >= 3) {
            listOf(PremenstrualMoodPattern)
        } else {
            emptyList()
        }
    }
}