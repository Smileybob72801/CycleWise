package com.veleda.cyclewise.domain.insights.charts

import com.veleda.cyclewise.domain.insights.analysis.CycleAnalyzer
import com.veleda.cyclewise.domain.models.FullDailyLog
import com.veleda.cyclewise.domain.models.Period
import com.veleda.cyclewise.domain.models.WaterIntake
import kotlinx.datetime.daysUntil

/**
 * Generates chart data models from user health data.
 *
 * All methods return null when insufficient data exists.
 * Chart data is platform-agnostic — Compose composables in `composeApp`
 * render these models using Vico.
 */
class ChartDataGenerator {

    companion object {
        private const val MIN_CYCLES_FOR_HISTORY = 3
        private const val MIN_CYCLES_FOR_COMPARISON = 2
        private const val PHASE_MENSTRUAL = "Menstrual"
        private const val PHASE_FOLLICULAR = "Follicular"
        private const val PHASE_OVULATORY = "Ovulatory"
        private const val PHASE_LUTEAL = "Luteal"
    }

    /**
     * Generates a line chart of cycle lengths over time.
     *
     * @return Line chart with cycle index on X and length in days on Y, or null.
     */
    fun cycleLengthHistory(periods: List<Period>): LineChartData? {
        val boundaries = CycleAnalyzer.buildCycleBoundaries(periods)
        if (boundaries.size < MIN_CYCLES_FOR_HISTORY) return null

        val points = boundaries.mapIndexed { index, boundary ->
            ChartPoint(
                x = (index + 1).toFloat(),
                y = boundary.cycleLength.toFloat(),
                label = "Cycle ${index + 1}",
            )
        }

        return LineChartData(
            title = "Cycle Length History",
            key = "chart-cycle-length-history",
            series = listOf(ChartSeries(label = "Length (days)", points = points)),
            xAxisLabel = "Cycle",
            yAxisLabel = "Days",
        )
    }

    /**
     * Generates a bar chart of average mood score by cycle phase.
     *
     * @return Bar chart with phase on X and average mood (1-5) on Y, or null.
     */
    fun moodAcrossPhases(
        periods: List<Period>,
        allLogs: List<FullDailyLog>,
    ): BarChartData? {
        val phaseAverages = computePhaseAverages(periods, allLogs) { it.entry.moodScore }
            ?: return null

        return BarChartData(
            title = "Mood by Phase",
            key = "chart-mood-by-phase",
            bars = phaseAverages,
            xAxisLabel = "Phase",
            yAxisLabel = "Avg Mood",
        )
    }

    /**
     * Generates a bar chart of average energy level by cycle phase.
     *
     * @return Bar chart with phase on X and average energy (1-5) on Y, or null.
     */
    fun energyAcrossPhases(
        periods: List<Period>,
        allLogs: List<FullDailyLog>,
    ): BarChartData? {
        val phaseAverages = computePhaseAverages(periods, allLogs) { it.entry.energyLevel }
            ?: return null

        return BarChartData(
            title = "Energy by Phase",
            key = "chart-energy-by-phase",
            bars = phaseAverages,
            xAxisLabel = "Phase",
            yAxisLabel = "Avg Energy",
        )
    }

    /**
     * Generates a bar chart of flow intensity distribution across period days.
     *
     * Maps flow intensity to a numeric value (LIGHT=1, MEDIUM=2, HEAVY=3)
     * and shows the average per period day.
     *
     * @return Bar chart with day-in-period on X and avg intensity on Y, or null.
     */
    fun flowIntensityByDay(
        periods: List<Period>,
        allLogs: List<FullDailyLog>,
    ): BarChartData? {
        val boundaries = CycleAnalyzer.buildCycleBoundaries(periods)
        if (boundaries.size < MIN_CYCLES_FOR_COMPARISON) return null

        val avgPeriodLength = periods
            .filter { it.endDate != null }
            .map { it.startDate.daysUntil(it.endDate!!) + 1 }
            .average()
            .toInt()
            .coerceAtLeast(1)

        val dayIntensities = mutableMapOf<Int, MutableList<Float>>()
        for (boundary in boundaries) {
            val cycleLogs = allLogs.filter {
                it.entry.entryDate >= boundary.startDate && it.entry.entryDate < boundary.nextStartDate
            }
            for (log in cycleLogs) {
                val day = log.entry.dayInCycle
                if (day < 1 || day > avgPeriodLength) continue
                val intensity = log.periodLog?.flowIntensity ?: continue
                val value = flowIntensityToFloat(intensity)
                dayIntensities.getOrPut(day) { mutableListOf() }.add(value)
            }
        }

        if (dayIntensities.isEmpty()) return null

        val bars = (1..avgPeriodLength).mapNotNull { day ->
            val values = dayIntensities[day] ?: return@mapNotNull null
            ChartBar(label = "Day $day", value = values.average().toFloat())
        }

        if (bars.isEmpty()) return null

        return BarChartData(
            title = "Flow Intensity by Day",
            key = "chart-flow-intensity-by-day",
            bars = bars,
            xAxisLabel = "Period Day",
            yAxisLabel = "Avg Intensity",
        )
    }

    /**
     * Generates a line chart comparing key metrics across the last N cycles.
     *
     * Shows mood and energy averages per cycle for visual comparison.
     *
     * @return Line chart with cycle index on X and average scores on Y, or null.
     */
    fun cycleComparison(
        periods: List<Period>,
        allLogs: List<FullDailyLog>,
    ): LineChartData? {
        val boundaries = CycleAnalyzer.buildCycleBoundaries(periods)
        if (boundaries.size < MIN_CYCLES_FOR_COMPARISON) return null

        val moodPoints = mutableListOf<ChartPoint>()
        val energyPoints = mutableListOf<ChartPoint>()

        boundaries.forEachIndexed { index, boundary ->
            val cycleLogs = allLogs.filter {
                it.entry.entryDate >= boundary.startDate && it.entry.entryDate < boundary.nextStartDate
            }
            val moodAvg = cycleLogs.mapNotNull { it.entry.moodScore }.average()
            val energyAvg = cycleLogs.mapNotNull { it.entry.energyLevel }.average()

            if (!moodAvg.isNaN()) {
                moodPoints.add(ChartPoint(x = (index + 1).toFloat(), y = moodAvg.toFloat()))
            }
            if (!energyAvg.isNaN()) {
                energyPoints.add(ChartPoint(x = (index + 1).toFloat(), y = energyAvg.toFloat()))
            }
        }

        val series = buildList {
            if (moodPoints.isNotEmpty()) add(ChartSeries(label = "Mood", points = moodPoints))
            if (energyPoints.isNotEmpty()) add(ChartSeries(label = "Energy", points = energyPoints))
        }
        if (series.isEmpty()) return null

        return LineChartData(
            title = "Cycle Comparison",
            key = "chart-cycle-comparison",
            series = series,
            xAxisLabel = "Cycle",
            yAxisLabel = "Avg Score (1-5)",
        )
    }

    /**
     * Generates a bar chart of symptom frequency grouped by cycle phase.
     *
     * @return Bar chart with phase on X and total symptom count on Y, or null.
     */
    fun symptomFrequencyByPhase(
        periods: List<Period>,
        allLogs: List<FullDailyLog>,
    ): BarChartData? {
        val boundaries = CycleAnalyzer.buildCycleBoundaries(periods)
        if (boundaries.size < MIN_CYCLES_FOR_COMPARISON) return null

        val avgPeriodLength = periods
            .filter { it.endDate != null }
            .map { it.startDate.daysUntil(it.endDate!!) + 1 }
            .average()
            .toInt()
            .coerceAtLeast(1)

        val phaseCounts = mutableMapOf(
            PHASE_MENSTRUAL to 0,
            PHASE_FOLLICULAR to 0,
            PHASE_OVULATORY to 0,
            PHASE_LUTEAL to 0,
        )

        for (boundary in boundaries) {
            val cycleLogs = allLogs.filter {
                it.entry.entryDate >= boundary.startDate && it.entry.entryDate < boundary.nextStartDate
            }
            for (log in cycleLogs) {
                val count = log.symptomLogs.size
                if (count == 0) continue
                val phase = classifyPhase(log.entry.dayInCycle, boundary.cycleLength, avgPeriodLength)
                phaseCounts[phase] = (phaseCounts[phase] ?: 0) + count
            }
        }

        if (phaseCounts.values.all { it == 0 }) return null

        val bars = listOf(PHASE_MENSTRUAL, PHASE_FOLLICULAR, PHASE_OVULATORY, PHASE_LUTEAL).map {
            ChartBar(label = it, value = (phaseCounts[it] ?: 0).toFloat())
        }

        return BarChartData(
            title = "Symptoms by Phase",
            key = "chart-symptoms-by-phase",
            bars = bars,
            xAxisLabel = "Phase",
            yAxisLabel = "Total Count",
        )
    }

    /**
     * Generates a bar chart of average water intake by cycle phase.
     *
     * @return Bar chart with phase on X and average cups on Y, or null.
     */
    fun waterIntakeByPhase(
        periods: List<Period>,
        allLogs: List<FullDailyLog>,
        waterIntakes: List<WaterIntake>,
    ): BarChartData? {
        val boundaries = CycleAnalyzer.buildCycleBoundaries(periods)
        if (boundaries.size < MIN_CYCLES_FOR_COMPARISON) return null

        val avgPeriodLength = periods
            .filter { it.endDate != null }
            .map { it.startDate.daysUntil(it.endDate!!) + 1 }
            .average()
            .toInt()
            .coerceAtLeast(1)

        val waterByDate = waterIntakes.associateBy { it.date }
        val phaseValues = mutableMapOf<String, MutableList<Float>>()

        for (boundary in boundaries) {
            val cycleLogs = allLogs.filter {
                it.entry.entryDate >= boundary.startDate && it.entry.entryDate < boundary.nextStartDate
            }
            for (log in cycleLogs) {
                val water = waterByDate[log.entry.entryDate] ?: continue
                val phase = classifyPhase(log.entry.dayInCycle, boundary.cycleLength, avgPeriodLength)
                phaseValues.getOrPut(phase) { mutableListOf() }.add(water.cups.toFloat())
            }
        }

        if (phaseValues.isEmpty()) return null

        val bars = listOf(PHASE_MENSTRUAL, PHASE_FOLLICULAR, PHASE_OVULATORY, PHASE_LUTEAL).map {
            val avg = phaseValues[it]?.average()?.toFloat() ?: 0f
            ChartBar(label = it, value = avg)
        }

        return BarChartData(
            title = "Water Intake by Phase",
            key = "chart-water-by-phase",
            bars = bars,
            xAxisLabel = "Phase",
            yAxisLabel = "Avg Cups",
        )
    }

    /**
     * Generates all charts that have sufficient data.
     *
     * @return List of chart data objects, only those with enough data.
     */
    fun generateAll(
        periods: List<Period>,
        allLogs: List<FullDailyLog>,
        waterIntakes: List<WaterIntake>,
    ): List<ChartData> = listOfNotNull(
        cycleLengthHistory(periods),
        moodAcrossPhases(periods, allLogs),
        energyAcrossPhases(periods, allLogs),
        flowIntensityByDay(periods, allLogs),
        symptomFrequencyByPhase(periods, allLogs),
        waterIntakeByPhase(periods, allLogs, waterIntakes),
        cycleComparison(periods, allLogs),
    )

    /**
     * Computes average values for a numeric metric grouped by cycle phase.
     */
    private fun computePhaseAverages(
        periods: List<Period>,
        allLogs: List<FullDailyLog>,
        valueExtractor: (FullDailyLog) -> Int?,
    ): List<ChartBar>? {
        val boundaries = CycleAnalyzer.buildCycleBoundaries(periods)
        if (boundaries.size < MIN_CYCLES_FOR_COMPARISON) return null

        val avgPeriodLength = periods
            .filter { it.endDate != null }
            .map { it.startDate.daysUntil(it.endDate!!) + 1 }
            .average()
            .toInt()
            .coerceAtLeast(1)

        val phaseValues = mutableMapOf<String, MutableList<Float>>()

        for (boundary in boundaries) {
            val cycleLogs = allLogs.filter {
                it.entry.entryDate >= boundary.startDate && it.entry.entryDate < boundary.nextStartDate
            }
            for (log in cycleLogs) {
                val value = valueExtractor(log) ?: continue
                val phase = classifyPhase(log.entry.dayInCycle, boundary.cycleLength, avgPeriodLength)
                phaseValues.getOrPut(phase) { mutableListOf() }.add(value.toFloat())
            }
        }

        if (phaseValues.isEmpty()) return null

        return listOf(PHASE_MENSTRUAL, PHASE_FOLLICULAR, PHASE_OVULATORY, PHASE_LUTEAL).map {
            val avg = phaseValues[it]?.average()?.toFloat() ?: 0f
            ChartBar(label = it, value = avg)
        }
    }

    /**
     * Classifies a day-in-cycle into one of four cycle phases.
     *
     * - Menstrual: days 1 through avgPeriodLength
     * - Follicular: days (avgPeriodLength+1) through (cycleLength/2 - 1)
     * - Ovulatory: days (cycleLength/2 - 1) through (cycleLength/2 + 1)
     * - Luteal: days (cycleLength/2 + 2) through cycleLength
     */
    private fun classifyPhase(dayInCycle: Int, cycleLength: Int, avgPeriodLength: Int): String {
        val midpoint = cycleLength / 2
        return when {
            dayInCycle <= avgPeriodLength -> PHASE_MENSTRUAL
            dayInCycle < midpoint - 1 -> PHASE_FOLLICULAR
            dayInCycle <= midpoint + 1 -> PHASE_OVULATORY
            else -> PHASE_LUTEAL
        }
    }

    @Suppress("MagicNumber")
    private fun flowIntensityToFloat(intensity: com.veleda.cyclewise.domain.models.FlowIntensity): Float =
        when (intensity) {
            com.veleda.cyclewise.domain.models.FlowIntensity.LIGHT -> 1f
            com.veleda.cyclewise.domain.models.FlowIntensity.MEDIUM -> 2f
            com.veleda.cyclewise.domain.models.FlowIntensity.HEAVY -> 3f
        }
}
