package com.veleda.cyclewise.domain.models

import kotlinx.datetime.LocalDate
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class HeatmapMetricTest {

    private val baseData = DayHeatmapData(date = LocalDate(2025, 6, 15))

    // ── Mood ─────────────────────────────────────────────────────────────

    @Test
    fun `mood WHEN score1 THEN intensity0`() {
        assertEquals(0f, HeatmapMetric.Mood.intensity(baseData.copy(moodScore = 1)))
    }

    @Test
    fun `mood WHEN score5 THEN intensity1`() {
        assertEquals(1f, HeatmapMetric.Mood.intensity(baseData.copy(moodScore = 5)))
    }

    @Test
    fun `mood WHEN null THEN returnsNull`() {
        assertNull(HeatmapMetric.Mood.intensity(baseData))
    }

    // ── Energy ───────────────────────────────────────────────────────────

    @Test
    fun `energy WHEN level3 THEN intensity05`() {
        assertEquals(0.5f, HeatmapMetric.Energy.intensity(baseData.copy(energyLevel = 3)))
    }

    @Test
    fun `energy WHEN null THEN returnsNull`() {
        assertNull(HeatmapMetric.Energy.intensity(baseData))
    }

    // ── Libido ───────────────────────────────────────────────────────────

    @Test
    fun `libido WHEN score1 THEN intensity0`() {
        assertEquals(0f, HeatmapMetric.Libido.intensity(baseData.copy(libidoScore = 1)))
    }

    @Test
    fun `libido WHEN null THEN returnsNull`() {
        assertNull(HeatmapMetric.Libido.intensity(baseData))
    }

    // ── WaterIntake ──────────────────────────────────────────────────────

    @Test
    fun `waterIntake WHEN 4cups THEN intensity05`() {
        assertEquals(0.5f, HeatmapMetric.WaterIntake.intensity(baseData.copy(waterCups = 4)))
    }

    @Test
    fun `waterIntake WHEN 10cups THEN clampsTo1`() {
        assertEquals(1f, HeatmapMetric.WaterIntake.intensity(baseData.copy(waterCups = 10)))
    }

    @Test
    fun `waterIntake WHEN null THEN returnsNull`() {
        assertNull(HeatmapMetric.WaterIntake.intensity(baseData))
    }

    // ── SymptomSeverity ──────────────────────────────────────────────────

    @Test
    fun `symptomSeverity WHEN severity5 THEN intensity1`() {
        assertEquals(1f, HeatmapMetric.SymptomSeverity.intensity(baseData.copy(symptomMaxSeverity = 5)))
    }

    @Test
    fun `symptomSeverity WHEN null THEN returnsNull`() {
        assertNull(HeatmapMetric.SymptomSeverity.intensity(baseData))
    }

    // ── FlowIntensity ────────────────────────────────────────────────────

    @Test
    fun `flowIntensity WHEN light THEN returnsPoint33`() {
        assertEquals(0.33f, HeatmapMetric.FlowIntensity.intensity(baseData.copy(flowIntensity = FlowIntensity.LIGHT)))
    }

    @Test
    fun `flowIntensity WHEN heavy THEN returns1`() {
        assertEquals(1f, HeatmapMetric.FlowIntensity.intensity(baseData.copy(flowIntensity = FlowIntensity.HEAVY)))
    }

    @Test
    fun `flowIntensity WHEN null THEN returnsNull`() {
        assertNull(HeatmapMetric.FlowIntensity.intensity(baseData))
    }

    // ── MedicationCount ──────────────────────────────────────────────────

    @Test
    fun `medicationCount WHEN 0 THEN returnsNull`() {
        assertNull(HeatmapMetric.MedicationCount.intensity(baseData.copy(medicationCount = 0)))
    }

    @Test
    fun `medicationCount WHEN 5 THEN returns1`() {
        assertEquals(1f, HeatmapMetric.MedicationCount.intensity(baseData.copy(medicationCount = 5)))
    }

    @Test
    fun `medicationCount WHEN 2 THEN returnsPoint4`() {
        assertEquals(0.4f, HeatmapMetric.MedicationCount.intensity(baseData.copy(medicationCount = 2)))
    }
}
