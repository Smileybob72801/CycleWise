package com.veleda.cyclewise.domain.insights

import com.veleda.cyclewise.domain.models.Period

/**
 * Status of data readiness for a specific insight category.
 *
 * @property category      The insight category this readiness applies to.
 * @property label         Human-readable label for display.
 * @property isReady       Whether enough data is available.
 * @property completedPeriods Number of completed periods currently available.
 * @property periodsRequired Minimum completed periods needed for this category.
 * @property periodsRemaining How many more completed periods are needed, or 0 if ready.
 */
data class DataReadiness(
    val category: InsightCategory,
    val label: String,
    val isReady: Boolean,
    val completedPeriods: Int,
    val periodsRequired: Int,
    val periodsRemaining: Int,
)

/**
 * Calculates data readiness for each insight category based on available period data.
 *
 * Thresholds:
 * - SUMMARY: 1 period
 * - PREDICTION: 2 completed periods
 * - PATTERN: 2 completed periods
 * - TREND: 3 completed periods
 * - CORRELATION: 2 completed periods + 30 daily logs with paired data
 */
class DataReadinessCalculator {

    companion object {
        private const val SUMMARY_THRESHOLD = 1
        private const val PREDICTION_THRESHOLD = 2
        private const val PATTERN_THRESHOLD = 2
        private const val TREND_THRESHOLD = 3
        private const val CORRELATION_THRESHOLD = 2
    }

    /**
     * Computes data readiness for all insight categories.
     *
     * @param allPeriods All periods (ongoing and completed).
     * @return One [DataReadiness] per category, sorted by readiness (not-ready first).
     */
    fun calculate(allPeriods: List<Period>): List<DataReadiness> {
        val completedPeriods = allPeriods.count { it.endDate != null }
        val totalPeriods = allPeriods.size

        return listOf(
            DataReadiness(
                category = InsightCategory.SUMMARY,
                label = "Cycle Summary",
                isReady = totalPeriods >= SUMMARY_THRESHOLD,
                completedPeriods = totalPeriods,
                periodsRequired = SUMMARY_THRESHOLD,
                periodsRemaining = (SUMMARY_THRESHOLD - totalPeriods).coerceAtLeast(0),
            ),
            DataReadiness(
                category = InsightCategory.PREDICTION,
                label = "Period Predictions",
                isReady = completedPeriods >= PREDICTION_THRESHOLD,
                completedPeriods = completedPeriods,
                periodsRequired = PREDICTION_THRESHOLD,
                periodsRemaining = (PREDICTION_THRESHOLD - completedPeriods).coerceAtLeast(0),
            ),
            DataReadiness(
                category = InsightCategory.PATTERN,
                label = "Phase Patterns",
                isReady = completedPeriods >= PATTERN_THRESHOLD,
                completedPeriods = completedPeriods,
                periodsRequired = PATTERN_THRESHOLD,
                periodsRemaining = (PATTERN_THRESHOLD - completedPeriods).coerceAtLeast(0),
            ),
            DataReadiness(
                category = InsightCategory.TREND,
                label = "Trend Analysis",
                isReady = completedPeriods >= TREND_THRESHOLD,
                completedPeriods = completedPeriods,
                periodsRequired = TREND_THRESHOLD,
                periodsRemaining = (TREND_THRESHOLD - completedPeriods).coerceAtLeast(0),
            ),
            DataReadiness(
                category = InsightCategory.CORRELATION,
                label = "Cross-Variable Correlations",
                isReady = completedPeriods >= CORRELATION_THRESHOLD,
                completedPeriods = completedPeriods,
                periodsRequired = CORRELATION_THRESHOLD,
                periodsRemaining = (CORRELATION_THRESHOLD - completedPeriods).coerceAtLeast(0),
            ),
        ).sortedBy { it.isReady }
    }
}
