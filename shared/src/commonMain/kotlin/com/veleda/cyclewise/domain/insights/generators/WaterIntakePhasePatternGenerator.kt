package com.veleda.cyclewise.domain.insights.generators

import com.veleda.cyclewise.domain.insights.Insight
import com.veleda.cyclewise.domain.insights.WaterIntakePhasePattern
import com.veleda.cyclewise.domain.insights.analysis.CycleAnalyzer
import com.veleda.cyclewise.domain.insights.analysis.PhaseDescriptionFormatter
import com.veleda.cyclewise.domain.models.FullDailyLog
import kotlinx.datetime.daysUntil

/**
 * Detects recurring water intake patterns across cycle phases.
 *
 * Joins [InsightData.waterIntakes] by date with daily logs, classifies each day's
 * intake as "low" (< 4 cups) or "high" (>= 6 cups), then applies the standard
 * phase-pattern analysis. Uses `maxGap = 2` and `minBlockSize = 3`.
 *
 * ## Minimum Data
 * Requires at least 2 completed periods and water intake data.
 */
class WaterIntakePhasePatternGenerator : InsightGenerator {

    companion object {
        private const val LOW_THRESHOLD = 4
        private const val HIGH_THRESHOLD = 6
    }

    override fun generate(data: InsightData): List<Insight> {
        val boundaries = CycleAnalyzer.buildCycleBoundaries(data.allPeriods)
        if (boundaries.isEmpty()) return emptyList()

        val waterByDate = data.waterIntakes.associateBy { it.date }
        if (waterByDate.isEmpty()) return emptyList()

        val extractor: (FullDailyLog, Int) -> List<Pair<String, Int>> = { log, cycleLength ->
            val water = waterByDate[log.entry.entryDate]
            if (water != null) {
                val normalizedDay = CycleAnalyzer.normalizeDay(log.entry.dayInCycle, cycleLength)
                when {
                    water.cups < LOW_THRESHOLD -> listOf(Pair("low", normalizedDay))
                    water.cups >= HIGH_THRESHOLD -> listOf(Pair("high", normalizedDay))
                    else -> emptyList()
                }
            } else {
                emptyList()
            }
        }

        val chronologicalPeriods = data.allPeriods.filter { it.endDate != null }.reversed()
        val resultingInsights = mutableListOf<WaterIntakePhasePattern>()

        for (intakeLevel in listOf("low", "high")) {
            val levelExtractor: (FullDailyLog, Int) -> List<Pair<String, Int>> = { log, cycleLength ->
                extractor(log, cycleLength).filter { it.first == intakeLevel }
            }
            val tally = CycleAnalyzer.tallyPatterns(boundaries, data.allLogs, levelExtractor)
            val significant = CycleAnalyzer.filterSignificant(tally, boundaries.size)

            val blocks = CycleAnalyzer.buildRecurrenceBlocks(
                significantPatterns = significant,
                totalCycles = boundaries.size,
                maxGap = 2,
                minBlockSize = 3,
            )

            for (block in blocks) {
                resultingInsights.add(
                    WaterIntakePhasePattern(
                        intakeLevel = intakeLevel,
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
