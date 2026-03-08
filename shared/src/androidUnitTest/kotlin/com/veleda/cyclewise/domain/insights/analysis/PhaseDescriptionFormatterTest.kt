package com.veleda.cyclewise.domain.insights.analysis

import com.veleda.cyclewise.testutil.buildPeriod
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class PhaseDescriptionFormatterTest {

    private val baseDate = LocalDate(2025, 1, 1)

    private val periods = listOf(
        buildPeriod(startDate = baseDate, endDate = baseDate.plus(4, DateTimeUnit.DAY)),
        buildPeriod(startDate = baseDate.plus(28, DateTimeUnit.DAY), endDate = baseDate.plus(32, DateTimeUnit.DAY)),
    )

    // ── isPeriodPhase ──

    @Test
    fun isPeriodPhase_WHEN_allDaysWithinPeriodLength_THEN_returnsTrue() {
        assertTrue(PhaseDescriptionFormatter.isPeriodPhase(listOf(1, 2, 3), 5.0))
    }

    @Test
    fun isPeriodPhase_WHEN_dayExceedsPeriodLength_THEN_returnsFalse() {
        assertFalse(PhaseDescriptionFormatter.isPeriodPhase(listOf(1, 2, 6), 5.0))
    }

    @Test
    fun isPeriodPhase_WHEN_negativeDays_THEN_returnsFalse() {
        assertFalse(PhaseDescriptionFormatter.isPeriodPhase(listOf(-3, -2, -1), 5.0))
    }

    @Test
    fun isPeriodPhase_WHEN_zeroDay_THEN_returnsFalse() {
        assertFalse(PhaseDescriptionFormatter.isPeriodPhase(listOf(0, 1, 2), 5.0))
    }

    // ── format (period symptoms) ──

    @Test
    fun format_WHEN_singlePeriodDay_THEN_formatsSingular() {
        val result = PhaseDescriptionFormatter.format(listOf(2), isPeriodPhase = true, periods)

        assertEquals("on day 2 of your period", result)
    }

    @Test
    fun format_WHEN_twoPeriodDays_THEN_formatsWithAmpersand() {
        val result = PhaseDescriptionFormatter.format(listOf(1, 3), isPeriodPhase = true, periods)

        assertEquals("on days 1 & 3 of your period", result)
    }

    @Test
    fun format_WHEN_threePeriodDays_THEN_formatsRange() {
        val result = PhaseDescriptionFormatter.format(listOf(1, 2, 3), isPeriodPhase = true, periods)

        assertEquals("on days 1-3 of your period", result)
    }

    @Test
    fun format_WHEN_emptyGroup_THEN_returnsEmpty() {
        assertEquals("", PhaseDescriptionFormatter.format(emptyList(), isPeriodPhase = true, periods))
    }

    // ── formatRelativePhase (luteal) ──

    @Test
    fun formatRelativePhase_WHEN_singleDayBeforePeriod_THEN_formatsRelative() {
        val result = PhaseDescriptionFormatter.formatRelativePhase(listOf(-1), periods)

        assertEquals("on the day before your period", result)
    }

    @Test
    fun formatRelativePhase_WHEN_singleLutealDay_THEN_formatsDaysBefore() {
        val result = PhaseDescriptionFormatter.formatRelativePhase(listOf(-5), periods)

        assertEquals("5 days before your period", result)
    }

    @Test
    fun formatRelativePhase_WHEN_multipleLutealDays_THEN_formatsRange() {
        val result = PhaseDescriptionFormatter.formatRelativePhase(listOf(-5, -4, -3), periods)

        assertEquals("from 3 to 5 days before your period", result)
    }

    // ── formatRelativePhase (follicular) ──

    @Test
    fun formatRelativePhase_WHEN_rightAfterPeriod_THEN_formatsRightAfter() {
        // Periods have endDate 4 days after start, daysUntil gives 4
        // Group day 5, avgPeriodLength = 4, so daysAfter = 5 - 4 = 1, <= 1 → "right after"
        val result = PhaseDescriptionFormatter.formatRelativePhase(listOf(5), periods)

        assertEquals("right after your period", result)
    }

    @Test
    fun formatRelativePhase_WHEN_singleFollicularDay_THEN_formatsDaysAfter() {
        // Group day 10, avgPeriodLength = 4, daysAfter = 10 - 4 = 6
        val result = PhaseDescriptionFormatter.formatRelativePhase(listOf(10), periods)

        assertEquals("6 days after your period", result)
    }

    @Test
    fun formatRelativePhase_WHEN_multipleFollicularDays_THEN_formatsRange() {
        // Days 10, 11, 12, avgPeriodLength = 4
        // firstDayAfter = 10 - 4 = 6, lastDayAfter = 12 - 4 = 8
        val result = PhaseDescriptionFormatter.formatRelativePhase(listOf(10, 11, 12), periods)

        assertEquals("from 6 to 8 days after your period", result)
    }

    @Test
    fun formatRelativePhase_WHEN_emptyGroup_THEN_returnsEmpty() {
        assertEquals("", PhaseDescriptionFormatter.formatRelativePhase(emptyList(), periods))
    }
}
