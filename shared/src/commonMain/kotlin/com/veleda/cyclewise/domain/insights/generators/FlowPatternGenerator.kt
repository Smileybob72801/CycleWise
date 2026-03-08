package com.veleda.cyclewise.domain.insights.generators

import com.veleda.cyclewise.domain.insights.FlowPattern
import com.veleda.cyclewise.domain.insights.Insight
import com.veleda.cyclewise.domain.insights.analysis.CycleAnalyzer
import com.veleda.cyclewise.domain.insights.analysis.DataExtractors
import com.veleda.cyclewise.domain.insights.analysis.PhaseDescriptionFormatter
import kotlinx.datetime.daysUntil

/**
 * Detects recurring flow intensity patterns across recent cycles.
 *
 * Analyzes flow intensity (light/medium/heavy) by normalized cycle day.
 * Groups consecutive days of the same intensity and produces [FlowPattern]
 * insights. Uses `maxGap = 1` and `minBlockSize = 1`.
 *
 * ## Minimum Data
 * Requires at least 2 completed periods (1 full cycle).
 */
class FlowPatternGenerator : InsightGenerator {

    override fun generate(data: InsightData): List<Insight> {
        val boundaries = CycleAnalyzer.buildCycleBoundaries(data.allPeriods)
        if (boundaries.isEmpty()) return emptyList()

        val tally = CycleAnalyzer.tallyPatterns(boundaries, data.allLogs, DataExtractors.flowIntensity)
        val significant = CycleAnalyzer.filterSignificant(tally, boundaries.size)
        if (significant.isEmpty()) return emptyList()

        val blocks = CycleAnalyzer.buildRecurrenceBlocks(
            significantPatterns = significant,
            totalCycles = boundaries.size,
            maxGap = 1,
            minBlockSize = 1,
        )

        val chronologicalPeriods = data.allPeriods.filter { it.endDate != null }.reversed()
        val avgPeriodLength = data.allPeriods
            .mapNotNull { it.endDate?.let { endDate -> it.startDate.daysUntil(endDate) + 1 } }
            .average()
            .takeIf { !it.isNaN() } ?: 5.0

        return blocks.map { block ->
            val isPeriodPhase = PhaseDescriptionFormatter.isPeriodPhase(
                block.normalizedDays, avgPeriodLength
            )
            FlowPattern(
                intensityType = block.dataId,
                phaseDescription = PhaseDescriptionFormatter.format(
                    block.normalizedDays, isPeriodPhase, chronologicalPeriods
                ),
                recurrenceRate = "${block.recurrenceCount} out of ${boundaries.size}",
            )
        }
    }
}
