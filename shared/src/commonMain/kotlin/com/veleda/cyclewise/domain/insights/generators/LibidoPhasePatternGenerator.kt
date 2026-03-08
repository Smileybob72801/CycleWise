package com.veleda.cyclewise.domain.insights.generators

import com.veleda.cyclewise.domain.insights.Insight
import com.veleda.cyclewise.domain.insights.LibidoPhasePattern
import com.veleda.cyclewise.domain.insights.analysis.CycleAnalyzer
import com.veleda.cyclewise.domain.insights.analysis.DataExtractors
import com.veleda.cyclewise.domain.insights.analysis.PhaseDescriptionFormatter

/**
 * Detects recurring libido-to-cycle-phase correlations across recent cycles.
 *
 * Uses the same algorithm as [MoodPhasePatternGenerator]: day normalization, tallying
 * with low/high thresholds (libido <= 2 / >= 4), significance filtering, and block
 * detection with `maxGap = 2` and `minBlockSize = 3`.
 *
 * ## Minimum Data
 * Requires at least 2 completed periods (1 full cycle).
 */
class LibidoPhasePatternGenerator : InsightGenerator {

    override fun generate(data: InsightData): List<Insight> {
        val boundaries = CycleAnalyzer.buildCycleBoundaries(data.allPeriods)
        if (boundaries.isEmpty()) return emptyList()

        val chronologicalPeriods = data.allPeriods.filter { it.endDate != null }.reversed()
        val resultingInsights = mutableListOf<LibidoPhasePattern>()

        for (libidoType in listOf("low", "high")) {
            val extractor = if (libidoType == "low") DataExtractors.libidoLow else DataExtractors.libidoHigh
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
                    LibidoPhasePattern(
                        libidoType = libidoType,
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
