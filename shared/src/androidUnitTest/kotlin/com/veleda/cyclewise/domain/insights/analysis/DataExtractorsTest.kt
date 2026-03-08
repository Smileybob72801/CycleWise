package com.veleda.cyclewise.domain.insights.analysis

import com.veleda.cyclewise.domain.models.FlowIntensity
import com.veleda.cyclewise.testutil.buildDailyEntry
import com.veleda.cyclewise.testutil.buildFullDailyLog
import com.veleda.cyclewise.testutil.buildPeriodLog
import com.veleda.cyclewise.testutil.buildSymptomLog
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class DataExtractorsTest {

    private val cycleLength = 28

    // ── symptoms ──

    @Test
    fun symptoms_WHEN_symptomLogsPresent_THEN_extractsSymptomIdAndNormalizedDay() {
        val log = buildFullDailyLog(
            entry = buildDailyEntry(dayInCycle = 3),
            symptomLogs = listOf(
                buildSymptomLog(symptomId = "headache"),
                buildSymptomLog(symptomId = "cramps"),
            ),
        )

        val result = DataExtractors.symptoms(log, cycleLength)

        assertEquals(2, result.size)
        assertEquals(Pair("headache", 3), result[0])
        assertEquals(Pair("cramps", 3), result[1])
    }

    @Test
    fun symptoms_WHEN_noSymptomLogs_THEN_returnsEmpty() {
        val log = buildFullDailyLog(entry = buildDailyEntry(dayInCycle = 3))

        val result = DataExtractors.symptoms(log, cycleLength)

        assertTrue(result.isEmpty())
    }

    // ── moodLow ──

    @Test
    fun moodLow_WHEN_moodScoreLow_THEN_extractsLow() {
        val log = buildFullDailyLog(entry = buildDailyEntry(dayInCycle = 5, moodScore = 2))

        val result = DataExtractors.moodLow(log, cycleLength)

        assertEquals(1, result.size)
        assertEquals(Pair("low", 5), result[0])
    }

    @Test
    fun moodLow_WHEN_moodScoreNeutral_THEN_returnsEmpty() {
        val log = buildFullDailyLog(entry = buildDailyEntry(dayInCycle = 5, moodScore = 3))

        assertTrue(DataExtractors.moodLow(log, cycleLength).isEmpty())
    }

    @Test
    fun moodLow_WHEN_noMoodScore_THEN_returnsEmpty() {
        val log = buildFullDailyLog(entry = buildDailyEntry(dayInCycle = 5))

        assertTrue(DataExtractors.moodLow(log, cycleLength).isEmpty())
    }

    // ── moodHigh ──

    @Test
    fun moodHigh_WHEN_moodScoreHigh_THEN_extractsHigh() {
        val log = buildFullDailyLog(entry = buildDailyEntry(dayInCycle = 10, moodScore = 4))

        val result = DataExtractors.moodHigh(log, cycleLength)

        assertEquals(1, result.size)
        assertEquals(Pair("high", 10), result[0])
    }

    @Test
    fun moodHigh_WHEN_moodScoreNeutral_THEN_returnsEmpty() {
        val log = buildFullDailyLog(entry = buildDailyEntry(dayInCycle = 10, moodScore = 3))

        assertTrue(DataExtractors.moodHigh(log, cycleLength).isEmpty())
    }

    // ── energyLow ──

    @Test
    fun energyLow_WHEN_energyLevelLow_THEN_extractsLow() {
        val log = buildFullDailyLog(entry = buildDailyEntry(dayInCycle = 2, energyLevel = 1))

        val result = DataExtractors.energyLow(log, cycleLength)

        assertEquals(1, result.size)
        assertEquals(Pair("low", 2), result[0])
    }

    @Test
    fun energyLow_WHEN_energyLevelHigh_THEN_returnsEmpty() {
        val log = buildFullDailyLog(entry = buildDailyEntry(dayInCycle = 2, energyLevel = 4))

        assertTrue(DataExtractors.energyLow(log, cycleLength).isEmpty())
    }

    // ── energyHigh ──

    @Test
    fun energyHigh_WHEN_energyLevelHigh_THEN_extractsHigh() {
        val log = buildFullDailyLog(entry = buildDailyEntry(dayInCycle = 12, energyLevel = 5))

        val result = DataExtractors.energyHigh(log, cycleLength)

        assertEquals(1, result.size)
        assertEquals(Pair("high", 12), result[0])
    }

    // ── libidoLow / libidoHigh ──

    @Test
    fun libidoLow_WHEN_libidoScoreLow_THEN_extractsLow() {
        val log = buildFullDailyLog(entry = buildDailyEntry(dayInCycle = 7, libidoScore = 1))

        val result = DataExtractors.libidoLow(log, cycleLength)

        assertEquals(1, result.size)
        assertEquals(Pair("low", 7), result[0])
    }

    @Test
    fun libidoHigh_WHEN_libidoScoreHigh_THEN_extractsHigh() {
        val log = buildFullDailyLog(entry = buildDailyEntry(dayInCycle = 14, libidoScore = 5))

        val result = DataExtractors.libidoHigh(log, cycleLength)

        assertEquals(1, result.size)
        assertEquals(Pair("high", 14), result[0])
    }

    // ── flowIntensity ──

    @Test
    fun flowIntensity_WHEN_flowPresent_THEN_extractsIntensityName() {
        val log = buildFullDailyLog(
            entry = buildDailyEntry(dayInCycle = 1),
            periodLog = buildPeriodLog(flowIntensity = FlowIntensity.HEAVY),
        )

        val result = DataExtractors.flowIntensity(log, cycleLength)

        assertEquals(1, result.size)
        assertEquals(Pair("heavy", 1), result[0])
    }

    @Test
    fun flowIntensity_WHEN_noPeriodLog_THEN_returnsEmpty() {
        val log = buildFullDailyLog(entry = buildDailyEntry(dayInCycle = 1))

        assertTrue(DataExtractors.flowIntensity(log, cycleLength).isEmpty())
    }

    @Test
    fun flowIntensity_WHEN_periodLogWithNullIntensity_THEN_returnsEmpty() {
        val log = buildFullDailyLog(
            entry = buildDailyEntry(dayInCycle = 1),
            periodLog = buildPeriodLog(flowIntensity = null),
        )

        assertTrue(DataExtractors.flowIntensity(log, cycleLength).isEmpty())
    }

    // ── normalizedDay for luteal phase ──

    @Test
    fun extractors_WHEN_lutealPhaseDay_THEN_normalizesToNegativeOffset() {
        val log = buildFullDailyLog(entry = buildDailyEntry(dayInCycle = 25, moodScore = 1))

        val result = DataExtractors.moodLow(log, cycleLength)

        // Day 25 in 28-day cycle: 25 > 16, so 25 - 28 - 1 = -4
        assertEquals(Pair("low", -4), result[0])
    }
}
