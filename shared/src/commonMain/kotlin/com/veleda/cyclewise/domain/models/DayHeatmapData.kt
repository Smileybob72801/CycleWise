package com.veleda.cyclewise.domain.models

import kotlinx.datetime.LocalDate

/**
 * Aggregated data for a single calendar day, used by [HeatmapMetric] to compute
 * overlay intensity values.
 *
 * @property date               Calendar date this data represents.
 * @property moodScore          Mood score (1-5), or null if unrecorded.
 * @property energyLevel        Energy level (1-5), or null if unrecorded.
 * @property libidoScore        Libido score (1-5), or null if unrecorded.
 * @property waterCups          Number of water cups logged, or null if unrecorded.
 * @property symptomCount       Number of symptoms logged on this day.
 * @property symptomMaxSeverity Highest severity among logged symptoms (1-5), or null.
 * @property flowIntensity      Menstrual flow intensity, or null if not a period day.
 * @property medicationCount    Number of medications logged on this day.
 */
data class DayHeatmapData(
    val date: LocalDate,
    val moodScore: Int? = null,
    val energyLevel: Int? = null,
    val libidoScore: Int? = null,
    val waterCups: Int? = null,
    val symptomCount: Int = 0,
    val symptomMaxSeverity: Int? = null,
    val flowIntensity: FlowIntensity? = null,
    val medicationCount: Int = 0,
)
