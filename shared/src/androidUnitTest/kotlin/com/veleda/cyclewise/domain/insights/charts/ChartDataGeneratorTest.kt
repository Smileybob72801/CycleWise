package com.veleda.cyclewise.domain.insights.charts

import com.veleda.cyclewise.domain.models.FlowIntensity
import com.veleda.cyclewise.domain.models.FullDailyLog
import com.veleda.cyclewise.domain.models.Period
import com.veleda.cyclewise.testutil.buildDailyEntry
import com.veleda.cyclewise.testutil.buildFullDailyLog
import com.veleda.cyclewise.testutil.buildPeriod
import com.veleda.cyclewise.testutil.buildPeriodLog
import com.veleda.cyclewise.testutil.buildSymptomLog
import com.veleda.cyclewise.testutil.buildWaterIntake
import kotlinx.datetime.LocalDate
import kotlin.time.ExperimentalTime
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalTime::class)
class ChartDataGeneratorTest {

    private val generator = ChartDataGenerator()

    /**
     * Builds 4 completed periods 28 days apart with daily logs.
     * Periods: Jan 1, Jan 29, Feb 26, Mar 26, Apr 23 (last one ongoing).
     */
    private fun buildPeriodsAndLogs(): Pair<List<Period>, List<FullDailyLog>> {
        val periodStarts = listOf(
            LocalDate(2025, 1, 1),
            LocalDate(2025, 1, 29),
            LocalDate(2025, 2, 26),
            LocalDate(2025, 3, 26),
            LocalDate(2025, 4, 23), // ongoing
        )
        val periodLength = 5
        val cycleLength = 28

        val periods = periodStarts.mapIndexed { i, start ->
            val end = if (i < periodStarts.size - 1) {
                LocalDate.fromEpochDays(start.toEpochDays() + periodLength - 1)
            } else null
            buildPeriod(startDate = start, endDate = end)
        }.sortedByDescending { it.startDate }

        val logs = mutableListOf<FullDailyLog>()
        for (i in 0 until periodStarts.size - 1) {
            val cycleStart = periodStarts[i]
            for (day in 1..cycleLength) {
                val date = LocalDate.fromEpochDays(cycleStart.toEpochDays() + day - 1)
                val isPeriodDay = day <= periodLength
                logs.add(
                    buildFullDailyLog(
                        entry = buildDailyEntry(
                            entryDate = date,
                            dayInCycle = day,
                            moodScore = (day % 5) + 1,
                            energyLevel = (day % 4) + 1,
                        ),
                        periodLog = if (isPeriodDay) buildPeriodLog(
                            flowIntensity = when {
                                day <= 2 -> FlowIntensity.LIGHT
                                day <= 4 -> FlowIntensity.HEAVY
                                else -> FlowIntensity.MEDIUM
                            }
                        ) else null,
                        symptomLogs = if (day <= 3) listOf(buildSymptomLog()) else emptyList(),
                    ),
                )
            }
        }

        return periods to logs
    }

    // ── cycleLengthHistory ──────────────────────────────────────────────

    @Test
    fun `cycleLengthHistory WHEN fewerThan3Cycles THEN returnsNull`() {
        val periods = listOf(
            buildPeriod(startDate = LocalDate(2025, 1, 1), endDate = LocalDate(2025, 1, 5)),
            buildPeriod(startDate = LocalDate(2025, 1, 29), endDate = LocalDate(2025, 2, 2)),
        ).sortedByDescending { it.startDate }

        assertNull(generator.cycleLengthHistory(periods))
    }

    @Test
    fun `cycleLengthHistory WHEN sufficientCycles THEN returnsLineChart`() {
        val (periods, _) = buildPeriodsAndLogs()
        val chart = generator.cycleLengthHistory(periods)

        assertNotNull(chart)
        assertIs<LineChartData>(chart)
        assertEquals("chart-cycle-length-history", chart.key)
        assertTrue(chart.series.isNotEmpty())
        assertTrue(chart.series[0].points.size >= 3)
    }

    // ── moodAcrossPhases ────────────────────────────────────────────────

    @Test
    fun `moodAcrossPhases WHEN noLogs THEN returnsNull`() {
        val (periods, _) = buildPeriodsAndLogs()
        assertNull(generator.moodAcrossPhases(periods, emptyList()))
    }

    @Test
    fun `moodAcrossPhases WHEN sufficientData THEN returnsBarChart`() {
        val (periods, logs) = buildPeriodsAndLogs()
        val chart = generator.moodAcrossPhases(periods, logs)

        assertNotNull(chart)
        assertIs<BarChartData>(chart)
        assertEquals("chart-mood-by-phase", chart.key)
        assertEquals(4, chart.bars.size)
    }

    // ── energyAcrossPhases ──────────────────────────────────────────────

    @Test
    fun `energyAcrossPhases WHEN sufficientData THEN returnsBarChart`() {
        val (periods, logs) = buildPeriodsAndLogs()
        val chart = generator.energyAcrossPhases(periods, logs)

        assertNotNull(chart)
        assertIs<BarChartData>(chart)
        assertEquals("chart-energy-by-phase", chart.key)
        assertEquals(4, chart.bars.size)
    }

    // ── flowIntensityByDay ──────────────────────────────────────────────

    @Test
    fun `flowIntensityByDay WHEN noFlowLogs THEN returnsNull`() {
        val (periods, _) = buildPeriodsAndLogs()
        assertNull(generator.flowIntensityByDay(periods, emptyList()))
    }

    @Test
    fun `flowIntensityByDay WHEN sufficientData THEN returnsBarChart`() {
        val (periods, logs) = buildPeriodsAndLogs()
        val chart = generator.flowIntensityByDay(periods, logs)

        assertNotNull(chart)
        assertIs<BarChartData>(chart)
        assertEquals("chart-flow-intensity-by-day", chart.key)
        assertTrue(chart.bars.isNotEmpty())
    }

    // ── cycleComparison ─────────────────────────────────────────────────

    @Test
    fun `cycleComparison WHEN sufficientData THEN returnsLineChart`() {
        val (periods, logs) = buildPeriodsAndLogs()
        val chart = generator.cycleComparison(periods, logs)

        assertNotNull(chart)
        assertIs<LineChartData>(chart)
        assertEquals("chart-cycle-comparison", chart.key)
        assertTrue(chart.series.isNotEmpty())
    }

    @Test
    fun `cycleComparison WHEN insufficientPeriods THEN returnsNull`() {
        val periods = listOf(
            buildPeriod(startDate = LocalDate(2025, 1, 1), endDate = LocalDate(2025, 1, 5)),
        )
        assertNull(generator.cycleComparison(periods, emptyList()))
    }

    // ── symptomFrequencyByPhase ─────────────────────────────────────────

    @Test
    fun `symptomFrequencyByPhase WHEN noSymptoms THEN returnsNull`() {
        val (periods, _) = buildPeriodsAndLogs()
        assertNull(generator.symptomFrequencyByPhase(periods, emptyList()))
    }

    @Test
    fun `symptomFrequencyByPhase WHEN sufficientData THEN returnsBarChart`() {
        val (periods, logs) = buildPeriodsAndLogs()
        val chart = generator.symptomFrequencyByPhase(periods, logs)

        assertNotNull(chart)
        assertIs<BarChartData>(chart)
        assertEquals("chart-symptoms-by-phase", chart.key)
        assertEquals(4, chart.bars.size)
    }

    // ── waterIntakeByPhase ──────────────────────────────────────────────

    @Test
    fun `waterIntakeByPhase WHEN noWaterData THEN returnsNull`() {
        val (periods, logs) = buildPeriodsAndLogs()
        assertNull(generator.waterIntakeByPhase(periods, logs, emptyList()))
    }

    @Test
    fun `waterIntakeByPhase WHEN sufficientData THEN returnsBarChart`() {
        val (periods, logs) = buildPeriodsAndLogs()
        val waterIntakes = logs.map { buildWaterIntake(date = it.entry.entryDate, cups = 6) }
        val chart = generator.waterIntakeByPhase(periods, logs, waterIntakes)

        assertNotNull(chart)
        assertIs<BarChartData>(chart)
        assertEquals("chart-water-by-phase", chart.key)
        assertEquals(4, chart.bars.size)
    }

    // ── generateAll ─────────────────────────────────────────────────────

    @Test
    fun `generateAll WHEN sufficientData THEN returnsMultipleCharts`() {
        val (periods, logs) = buildPeriodsAndLogs()
        val waterIntakes = logs.map { buildWaterIntake(date = it.entry.entryDate, cups = 6) }
        val charts = generator.generateAll(periods, logs, waterIntakes)

        assertTrue(charts.isNotEmpty(), "Should generate at least one chart")
    }

    @Test
    fun `generateAll WHEN insufficientData THEN returnsEmptyList`() {
        val charts = generator.generateAll(emptyList(), emptyList(), emptyList())
        assertTrue(charts.isEmpty())
    }
}
