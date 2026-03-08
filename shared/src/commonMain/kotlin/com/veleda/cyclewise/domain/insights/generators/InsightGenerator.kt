package com.veleda.cyclewise.domain.insights.generators

import com.veleda.cyclewise.domain.models.FullDailyLog
import com.veleda.cyclewise.domain.models.Medication
import com.veleda.cyclewise.domain.models.Period
import com.veleda.cyclewise.domain.models.Symptom
import com.veleda.cyclewise.domain.models.WaterIntake
import com.veleda.cyclewise.domain.insights.Insight

/**
 * Immutable snapshot of all data available to insight generators.
 *
 * Passed by the [InsightEngine] to each generator. Generators must treat this
 * as read-only and must not mutate any collections.
 *
 * @property allPeriods         All periods, sorted by start date descending.
 * @property allLogs            All daily logs across all dates.
 * @property symptomLibrary     The user's symptom library (for ID-to-name resolution).
 * @property averageCycleLength Pre-calculated average cycle length in days, or null if
 *                              fewer than 2 completed periods exist.
 * @property generatedInsights  Insights produced by earlier generators in the pipeline
 *                              (used for cross-generator dependencies, e.g., prediction date).
 * @property topSymptomsCount   User-configured limit for the "top symptoms" insight.
 */
data class InsightData(
    val allPeriods: List<Period>,
    val allLogs: List<FullDailyLog>,
    val symptomLibrary: List<Symptom>,
    val averageCycleLength: Double?,
    val generatedInsights: List<Insight> = emptyList(),
    val topSymptomsCount: Int,
    val waterIntakes: List<WaterIntake> = emptyList(),
    val medicationLibrary: List<Medication> = emptyList(),
)

/**
 * Contract for a class that generates zero or more [Insight]s from user data.
 *
 * Implementations must be **pure** — [generate] must not mutate [data] or produce
 * side effects. Each generator is responsible for its own minimum-data guards
 * (e.g., returning an empty list when insufficient data exists).
 */
interface InsightGenerator {
    /** Produces insights from [data], or an empty list if insufficient data is available. */
    fun generate(data: InsightData): List<Insight>
}