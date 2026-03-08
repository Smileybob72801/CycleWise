package com.veleda.cyclewise.domain.models

/**
 * A calendar heatmap overlay metric.
 *
 * Each metric defines a [label] for UI display, a [key] for persistence/tracking,
 * and an [intensity] function that maps a day's data to a 0.0-1.0 value (or null
 * if no data exists for that metric on the given day).
 *
 * Color gradients are mapped separately in the Compose UI layer (`HeatmapColors.kt`)
 * to keep this interface free of Compose dependencies.
 */
sealed interface HeatmapMetric {
    /** User-facing display label. */
    val label: String
    /** Stable identifier for this metric. */
    val key: String
    /** Returns a 0.0-1.0 intensity value, or null if no data exists for this day. */
    fun intensity(dayData: DayHeatmapData): Float?

    /** Mood score mapped to 0.0-1.0 (1→0.0, 5→1.0). */
    data object Mood : HeatmapMetric {
        override val label = "Mood"
        override val key = "mood"
        override fun intensity(dayData: DayHeatmapData): Float? =
            dayData.moodScore?.let { (it - 1) / 4f }
    }

    /** Energy level mapped to 0.0-1.0 (1→0.0, 5→1.0). */
    data object Energy : HeatmapMetric {
        override val label = "Energy"
        override val key = "energy"
        override fun intensity(dayData: DayHeatmapData): Float? =
            dayData.energyLevel?.let { (it - 1) / 4f }
    }

    /** Libido score mapped to 0.0-1.0 (1→0.0, 5→1.0). */
    data object Libido : HeatmapMetric {
        override val label = "Libido"
        override val key = "libido"
        override fun intensity(dayData: DayHeatmapData): Float? =
            dayData.libidoScore?.let { (it - 1) / 4f }
    }

    /** Water intake mapped to 0.0-1.0 (0→0.0, 8+→1.0). */
    data object WaterIntake : HeatmapMetric {
        override val label = "Water"
        override val key = "water"
        private const val MAX_CUPS = 8f
        override fun intensity(dayData: DayHeatmapData): Float? =
            dayData.waterCups?.let { (it / MAX_CUPS).coerceIn(0f, 1f) }
    }

    /** Symptom max severity mapped to 0.0-1.0 (1→0.0, 5→1.0). */
    data object SymptomSeverity : HeatmapMetric {
        override val label = "Symptom Severity"
        override val key = "symptom_severity"
        override fun intensity(dayData: DayHeatmapData): Float? =
            dayData.symptomMaxSeverity?.let { (it - 1) / 4f }
    }

    /** Flow intensity mapped to discrete levels (LIGHT→0.33, MEDIUM→0.66, HEAVY→1.0). */
    data object FlowIntensity : HeatmapMetric {
        override val label = "Flow"
        override val key = "flow"
        override fun intensity(dayData: DayHeatmapData): Float? =
            when (dayData.flowIntensity) {
                com.veleda.cyclewise.domain.models.FlowIntensity.LIGHT -> 0.33f
                com.veleda.cyclewise.domain.models.FlowIntensity.MEDIUM -> 0.66f
                com.veleda.cyclewise.domain.models.FlowIntensity.HEAVY -> 1.0f
                null -> null
            }
    }

    /** Medication count mapped to 0.0-1.0 (0→0.0, 5+→1.0). */
    data object MedicationCount : HeatmapMetric {
        override val label = "Medications"
        override val key = "medications"
        private const val MAX_MEDS = 5f
        override fun intensity(dayData: DayHeatmapData): Float? =
            if (dayData.medicationCount > 0) {
                (dayData.medicationCount / MAX_MEDS).coerceIn(0f, 1f)
            } else {
                null
            }
    }
}
