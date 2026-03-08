package com.veleda.cyclewise.domain.insights.analysis

import com.veleda.cyclewise.testutil.TestData
import com.veleda.cyclewise.testutil.buildDailyEntry
import com.veleda.cyclewise.testutil.buildFullDailyLog
import com.veleda.cyclewise.testutil.buildPeriod
import com.veleda.cyclewise.testutil.buildSymptomLog
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class CycleAnalyzerTest {

    // ── buildCycleBoundaries ──

    @Test
    fun buildCycleBoundaries_WHEN_twoCompletedPeriods_THEN_returnsOneBoundary() {
        // ARRANGE
        val p1Start = TestData.DATE.minus(28, DateTimeUnit.DAY)
        val p2Start = TestData.DATE
        val periods = listOf(
            buildPeriod(startDate = p2Start, endDate = p2Start.plus(4, DateTimeUnit.DAY)),
            buildPeriod(startDate = p1Start, endDate = p1Start.plus(4, DateTimeUnit.DAY)),
        )

        // ACT
        val boundaries = CycleAnalyzer.buildCycleBoundaries(periods)

        // ASSERT
        assertEquals(1, boundaries.size)
        assertEquals(p1Start, boundaries[0].startDate)
        assertEquals(p2Start, boundaries[0].nextStartDate)
        assertEquals(28, boundaries[0].cycleLength)
    }

    @Test
    fun buildCycleBoundaries_WHEN_onePeriod_THEN_returnsEmpty() {
        val periods = listOf(
            buildPeriod(startDate = TestData.DATE, endDate = TestData.DATE.plus(4, DateTimeUnit.DAY)),
        )

        val boundaries = CycleAnalyzer.buildCycleBoundaries(periods)

        assertTrue(boundaries.isEmpty())
    }

    @Test
    fun buildCycleBoundaries_WHEN_incompletePeriods_THEN_excludesIncomplete() {
        val p1Start = TestData.DATE.minus(28, DateTimeUnit.DAY)
        val periods = listOf(
            buildPeriod(startDate = TestData.DATE, endDate = null), // ongoing
            buildPeriod(startDate = p1Start, endDate = p1Start.plus(4, DateTimeUnit.DAY)),
        )

        val boundaries = CycleAnalyzer.buildCycleBoundaries(periods)

        assertTrue(boundaries.isEmpty())
    }

    @Test
    fun buildCycleBoundaries_WHEN_moreThanMaxCycles_THEN_takesLastN() {
        // ARRANGE - 12 periods = 11 cycles, maxCycles = 3
        val baseDate = LocalDate(2024, 1, 1)
        val periods = (0..11).map { i ->
            val start = baseDate.plus(i * 28, DateTimeUnit.DAY)
            buildPeriod(startDate = start, endDate = start.plus(4, DateTimeUnit.DAY))
        }.reversed() // descending order

        // ACT
        val boundaries = CycleAnalyzer.buildCycleBoundaries(periods, maxCycles = 3)

        // ASSERT
        assertEquals(3, boundaries.size)
    }

    // ── normalizeDay ──

    @Test
    fun normalizeDay_WHEN_follicularPhase_THEN_returnsUnchanged() {
        assertEquals(5, CycleAnalyzer.normalizeDay(5, 28))
        assertEquals(16, CycleAnalyzer.normalizeDay(16, 28))
        assertEquals(1, CycleAnalyzer.normalizeDay(1, 30))
    }

    @Test
    fun normalizeDay_WHEN_lutealPhase_THEN_returnsNegativeOffset() {
        // Day 25 in a 28-day cycle: 25 - 28 - 1 = -4
        assertEquals(-4, CycleAnalyzer.normalizeDay(25, 28))
        // Day 28 in a 28-day cycle: 28 - 28 - 1 = -1
        assertEquals(-1, CycleAnalyzer.normalizeDay(28, 28))
        // Day 17 in a 28-day cycle: 17 - 28 - 1 = -12
        assertEquals(-12, CycleAnalyzer.normalizeDay(17, 28))
    }

    @Test
    fun normalizeDay_WHEN_customCutoff_THEN_usesCutoff() {
        // Day 10 with cutoff 8: 10 > 8, so 10 - 28 - 1 = -19
        assertEquals(-19, CycleAnalyzer.normalizeDay(10, 28, follicularCutoff = 8))
        // Day 8 with cutoff 8: 8 <= 8, kept
        assertEquals(8, CycleAnalyzer.normalizeDay(8, 28, follicularCutoff = 8))
    }

    // ── tallyPatterns ──

    @Test
    fun tallyPatterns_WHEN_symptomOccursInMultipleCycles_THEN_talliesCounts() {
        // ARRANGE
        val p1Start = TestData.DATE.minus(56, DateTimeUnit.DAY)
        val p2Start = TestData.DATE.minus(28, DateTimeUnit.DAY)
        val p3Start = TestData.DATE
        val boundaries = listOf(
            CycleBoundary(p1Start, p2Start, 28),
            CycleBoundary(p2Start, p3Start, 28),
        )

        val logs = listOf(
            buildFullDailyLog(
                entry = buildDailyEntry(entryDate = p1Start.plus(1, DateTimeUnit.DAY), dayInCycle = 2),
                symptomLogs = listOf(buildSymptomLog(symptomId = "headache")),
            ),
            buildFullDailyLog(
                entry = buildDailyEntry(entryDate = p2Start.plus(1, DateTimeUnit.DAY), dayInCycle = 2),
                symptomLogs = listOf(buildSymptomLog(symptomId = "headache")),
            ),
        )

        val extractor: (com.veleda.cyclewise.domain.models.FullDailyLog, Int) -> List<Pair<String, Int>> =
            { log, cycleLength ->
                log.symptomLogs.map { Pair(it.symptomId, CycleAnalyzer.normalizeDay(log.entry.dayInCycle, cycleLength)) }
            }

        // ACT
        val tally = CycleAnalyzer.tallyPatterns(boundaries, logs, extractor)

        // ASSERT
        assertEquals(2, tally[Pair("headache", 2)])
    }

    @Test
    fun tallyPatterns_WHEN_noLogsInCycle_THEN_skipsCycle() {
        val boundaries = listOf(
            CycleBoundary(LocalDate(2025, 1, 1), LocalDate(2025, 1, 29), 28),
        )

        val tally = CycleAnalyzer.tallyPatterns<String>(boundaries, emptyList()) { _, _ -> emptyList() }

        assertTrue(tally.isEmpty())
    }

    // ── filterSignificant ──

    @Test
    fun filterSignificant_WHEN_meetsThresholds_THEN_included() {
        val tally = mapOf("a" to 3, "b" to 1, "c" to 2)

        val result = CycleAnalyzer.filterSignificant(tally, totalCycles = 4, minCount = 1, minRecurrenceRate = 0.50)

        assertEquals(2, result.size)
        assertTrue("a" in result)
        assertTrue("c" in result)
    }

    @Test
    fun filterSignificant_WHEN_belowMinCount_THEN_excluded() {
        val tally = mapOf("a" to 1)

        val result = CycleAnalyzer.filterSignificant(tally, totalCycles = 1, minCount = 2)

        assertTrue(result.isEmpty())
    }

    @Test
    fun filterSignificant_WHEN_belowMinRate_THEN_excluded() {
        val tally = mapOf("a" to 1)

        val result = CycleAnalyzer.filterSignificant(tally, totalCycles = 5, minRecurrenceRate = 0.60)

        assertTrue(result.isEmpty())
    }

    // ── groupConsecutiveDays ──

    @Test
    fun groupConsecutiveDays_WHEN_strictConsecutive_THEN_groupsByGapOne() {
        val days = listOf(1, 2, 3, 7, 8)

        val groups = CycleAnalyzer.groupConsecutiveDays(days, maxGap = 1)

        assertEquals(2, groups.size)
        assertEquals(listOf(1, 2, 3), groups[0])
        assertEquals(listOf(7, 8), groups[1])
    }

    @Test
    fun groupConsecutiveDays_WHEN_relaxedGap_THEN_groupsByGapTwo() {
        val days = listOf(1, 2, 4, 5, 10)

        val groups = CycleAnalyzer.groupConsecutiveDays(days, maxGap = 2)

        assertEquals(2, groups.size)
        assertEquals(listOf(1, 2, 4, 5), groups[0])
        assertEquals(listOf(10), groups[1])
    }

    @Test
    fun groupConsecutiveDays_WHEN_empty_THEN_returnsEmpty() {
        assertTrue(CycleAnalyzer.groupConsecutiveDays(emptyList()).isEmpty())
    }

    @Test
    fun groupConsecutiveDays_WHEN_singleDay_THEN_returnsSingleGroup() {
        val groups = CycleAnalyzer.groupConsecutiveDays(listOf(5))

        assertEquals(1, groups.size)
        assertEquals(listOf(5), groups[0])
    }

    @Test
    fun groupConsecutiveDays_WHEN_negativeOffsets_THEN_groupsCorrectly() {
        val days = listOf(-5, -4, -3, -1)

        val groups = CycleAnalyzer.groupConsecutiveDays(days, maxGap = 1)

        assertEquals(2, groups.size)
        assertEquals(listOf(-5, -4, -3), groups[0])
        assertEquals(listOf(-1), groups[1])
    }

    // ── buildRecurrenceBlocks ──

    @Test
    fun buildRecurrenceBlocks_WHEN_sufficientPatterns_THEN_buildsBlocks() {
        val patterns = mapOf(
            Pair("headache", 1) to 3,
            Pair("headache", 2) to 2,
            Pair("headache", 3) to 3,
            Pair("cramps", 1) to 2,
        )

        val blocks = CycleAnalyzer.buildRecurrenceBlocks(patterns, totalCycles = 4, maxGap = 1, minBlockSize = 1)

        assertEquals(2, blocks.size)
        val headacheBlock = blocks.find { it.dataId == "headache" }!!
        assertEquals(listOf(1, 2, 3), headacheBlock.normalizedDays)
        assertEquals(2, headacheBlock.recurrenceCount) // min of 3, 2, 3
        assertEquals(4, headacheBlock.totalCycles)
        assertEquals(0.5, headacheBlock.recurrenceRate)
    }

    @Test
    fun buildRecurrenceBlocks_WHEN_belowMinBlockSize_THEN_filtered() {
        val patterns = mapOf(
            Pair("low", 1) to 2,
            Pair("low", 2) to 2,
        )

        val blocks = CycleAnalyzer.buildRecurrenceBlocks(patterns, totalCycles = 3, maxGap = 2, minBlockSize = 3)

        assertTrue(blocks.isEmpty())
    }

    @Test
    fun buildRecurrenceBlocks_WHEN_multipleDataIds_THEN_groupsSeparately() {
        val patterns = mapOf(
            Pair("low", 1) to 2,
            Pair("low", 2) to 2,
            Pair("low", 3) to 2,
            Pair("high", 10) to 3,
            Pair("high", 11) to 3,
            Pair("high", 12) to 3,
        )

        val blocks = CycleAnalyzer.buildRecurrenceBlocks(patterns, totalCycles = 3, maxGap = 2, minBlockSize = 3)

        assertEquals(2, blocks.size)
        assertTrue(blocks.any { it.dataId == "low" })
        assertTrue(blocks.any { it.dataId == "high" })
    }
}
