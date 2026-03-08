package com.veleda.cyclewise.domain.insights.generators

import com.veleda.cyclewise.domain.insights.Insight
import com.veleda.cyclewise.domain.insights.MoodPhasePattern
import com.veleda.cyclewise.domain.insights.analysis.CycleAnalyzer
import com.veleda.cyclewise.domain.insights.analysis.DataExtractors
import com.veleda.cyclewise.domain.insights.analysis.PhaseDescriptionFormatter

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
 * Same as symptom patterns: >= 1 occurrence AND >= 60% recurrence across analyzed cycles.
 *
 * ## Block Detection
 * Significant days are grouped by proximity (gap <= 2 days). A block must contain
 * at least 3 individually significant days to produce an insight.
 *
 * ## Minimum Data
 * Requires at least 2 completed periods (1 full cycle).
 */
class MoodPhasePatternGenerator : InsightGenerator {

    /**
     * Analyzes up to the last 8 cycles for recurring mood-to-phase correlations.
     *
     * Normalizes each mood occurrence's day-in-cycle to a phase-relative offset,
     * tallies low (score <= 2) and high (score >= 4) moods per normalized day,
     * then filters for days recurring in >= 60 % of cycles. Significant days
     * are grouped into blocks of >= 3 consecutive days to produce
     * [MoodPhasePattern] insights.
     *
     * @param data Aggregated cycle data; requires >= 2 completed periods.
     * @return Zero or more [MoodPhasePattern] insights, one per significant block.
     */
    override fun generate(data: InsightData): List<Insight> {
        val boundaries = CycleAnalyzer.buildCycleBoundaries(data.allPeriods)
        if (boundaries.isEmpty()) return emptyList()

        val lowTally = CycleAnalyzer.tallyPatterns(boundaries, data.allLogs, DataExtractors.moodLow)
        val highTally = CycleAnalyzer.tallyPatterns(boundaries, data.allLogs, DataExtractors.moodHigh)

        val combinedTally = (lowTally.keys + highTally.keys).associateWith { key ->
            (lowTally[key] ?: 0) + (highTally[key] ?: 0)
        }

        val significantPatterns = CycleAnalyzer.filterSignificant(combinedTally, boundaries.size)
        if (significantPatterns.isEmpty()) return emptyList()

        val chronologicalPeriods = data.allPeriods.filter { it.endDate != null }.reversed()
        val resultingInsights = mutableListOf<MoodPhasePattern>()

        for (moodType in listOf("low", "high")) {
            val tally = if (moodType == "low") lowTally else highTally
            val significantForMood = CycleAnalyzer.filterSignificant(tally, boundaries.size)

            val blocks = CycleAnalyzer.buildRecurrenceBlocks(
                significantPatterns = significantForMood,
                totalCycles = boundaries.size,
                maxGap = 2,
                minBlockSize = 3,
            )

            for (block in blocks) {
                resultingInsights.add(
                    MoodPhasePattern(
                        moodType = moodType,
                        phaseDescription = PhaseDescriptionFormatter.formatRelativePhase(
                            block.normalizedDays, chronologicalPeriods
                        ),
                        recurrenceRate = "${block.recurrenceCount} out of ${boundaries.size}",
                    )
                )
            }
        }

        return resultingInsights
    }
}
