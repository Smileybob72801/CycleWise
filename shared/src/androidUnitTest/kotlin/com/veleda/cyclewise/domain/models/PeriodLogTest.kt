package com.veleda.cyclewise.domain.models

import com.veleda.cyclewise.testutil.buildPeriodLog
import kotlin.time.ExperimentalTime
import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Unit tests for [PeriodLog.hasData].
 */
@OptIn(ExperimentalTime::class)
class PeriodLogTest {

    @Test
    fun `hasData WHEN allFieldsNull THEN returnsFalse`() {
        // GIVEN — a period log with no user-entered data
        val log = buildPeriodLog(
            flowIntensity = null,
            periodColor = null,
            periodConsistency = null,
        )

        // THEN
        assertFalse(log.hasData(), "hasData should be false when all fields are null")
    }

    @Test
    fun `hasData WHEN onlyFlowIntensityNonNull THEN returnsTrue`() {
        // GIVEN
        val log = buildPeriodLog(
            flowIntensity = FlowIntensity.MEDIUM,
            periodColor = null,
            periodConsistency = null,
        )

        // THEN
        assertTrue(log.hasData(), "hasData should be true when flowIntensity is non-null")
    }

    @Test
    fun `hasData WHEN onlyPeriodColorNonNull THEN returnsTrue`() {
        // GIVEN
        val log = buildPeriodLog(
            flowIntensity = null,
            periodColor = PeriodColor.DARK_RED,
            periodConsistency = null,
        )

        // THEN
        assertTrue(log.hasData(), "hasData should be true when periodColor is non-null")
    }

    @Test
    fun `hasData WHEN onlyPeriodConsistencyNonNull THEN returnsTrue`() {
        // GIVEN
        val log = buildPeriodLog(
            flowIntensity = null,
            periodColor = null,
            periodConsistency = PeriodConsistency.THICK,
        )

        // THEN
        assertTrue(log.hasData(), "hasData should be true when periodConsistency is non-null")
    }

    @Test
    fun `hasData WHEN multipleFieldsNonNull THEN returnsTrue`() {
        // GIVEN
        val log = buildPeriodLog(
            flowIntensity = FlowIntensity.HEAVY,
            periodColor = PeriodColor.BRIGHT_RED,
            periodConsistency = PeriodConsistency.MODERATE,
        )

        // THEN
        assertTrue(log.hasData(), "hasData should be true when multiple fields are non-null")
    }
}
