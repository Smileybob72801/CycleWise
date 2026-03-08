package com.veleda.cyclewise.domain.insights.generators

import com.veleda.cyclewise.domain.insights.EnergyPhasePattern
import com.veleda.cyclewise.domain.insights.Insight
import com.veleda.cyclewise.domain.insights.analysis.CycleAnalyzer
import com.veleda.cyclewise.domain.insights.analysis.DataExtractors
import com.veleda.cyclewise.domain.insights.analysis.PhaseDescriptionFormatter

/**
 * Detects recurring energy-to-cycle-phase correlations across recent cycles.
 *
 * Uses the same algorithm as [MoodPhasePatternGenerator]: day normalization, tallying
 * with low/high thresholds (energy <= 2 / >= 4), significance filtering, and block
 * detection with `maxGap = 2` and `minBlockSize = 3`.
 *
 * ## Minimum Data
 * Requires at least 2 completed periods (1 full cycle).
 */
class EnergyPhasePatternGenerator : InsightGenerator {

    override fun generate(data: InsightData): List<Insight> {
        val boundaries = CycleAnalyzer.buildCycleBoundaries(data.allPeriods)
        if (boundaries.isEmpty()) return emptyList()

        val chronologicalPeriods = data.allPeriods.filter { it.endDate != null }.reversed()
        val resultingInsights = mutableListOf<EnergyPhasePattern>()

        for (energyType in listOf("low", "high")) {
            val extractor = if (energyType == "low") DataExtractors.energyLow else DataExtractors.energyHigh
            val tally = CycleAnalyzer.tallyPatterns(boundaries, data.allLogs, extractor)
            val significant = CycleAnalyzer.filterSignificant(tally, boundaries.size)

            val blocks = CycleAnalyzer.buildRecurrenceBlocks(
                significantPatterns = significant,
                totalCycles = boundaries.size,
                maxGap = 2,
                minBlockSize = 3,
            )

            for (block in blocks) {
                resultingInsights.add(
                    EnergyPhasePattern(
                        energyType = energyType,
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
