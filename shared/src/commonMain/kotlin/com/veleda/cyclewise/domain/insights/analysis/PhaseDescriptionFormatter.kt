package com.veleda.cyclewise.domain.insights.analysis

import com.veleda.cyclewise.domain.models.Period
import kotlinx.datetime.daysUntil
import kotlin.math.abs

/**
 * Produces human-readable phase descriptions for cycle-pattern insights.
 *
 * Unifies the `formatPhaseDescription` and `formatRelativePhase` logic that was
 * previously duplicated between `SymptomPhasePatternGenerator` and
 * `MoodPhasePatternGenerator`. All formatting paths produce identical output to
 * the original implementations.
 */
object PhaseDescriptionFormatter {

    /**
     * Formats a full phase description for a group of normalized days.
     *
     * Period symptoms (all days within [avgPeriodLength]) receive "on day X of your period"
     * phrasing. Non-period symptoms are delegated to [formatRelativePhase].
     *
     * @param group           Sorted list of normalized day offsets forming one cluster.
     * @param isPeriodPhase   Whether all days fall within the average period length.
     * @param periods         Completed periods in chronological order (for avg period length calc).
     * @return Human-readable description, e.g., "on days 1-3 of your period".
     */
    fun format(
        group: List<Int>,
        isPeriodPhase: Boolean,
        periods: List<Period>,
    ): String {
        if (group.isEmpty()) return ""

        if (isPeriodPhase) {
            return when (group.size) {
                1 -> "on day ${group.first()} of your period"
                2 -> "on days ${group.first()} & ${group.last()} of your period"
                else -> "on days ${group.first()}-${group.last()} of your period"
            }
        }
        return formatRelativePhase(group, periods)
    }

    /**
     * Checks whether all normalized days in a group fall within the period phase.
     *
     * @param normalizedDays Sorted list of normalized day offsets.
     * @param avgPeriodLength Average period length in days.
     * @return `true` if every day is positive and within the average period length.
     */
    fun isPeriodPhase(normalizedDays: List<Int>, avgPeriodLength: Double): Boolean =
        normalizedDays.all { it > 0 && it <= avgPeriodLength }

    /**
     * Formats a relative phase description ("X days before/after your period").
     *
     * Uses the representative day (group average) to determine the phase:
     * - **Luteal phase** (representativeDay > 16 or < 0): "X days before your period"
     * - **Follicular phase** (representativeDay <= 16): "X days after your period"
     *
     * @param group   Sorted list of normalized day offsets forming one cluster.
     * @param periods Completed periods in chronological order.
     * @return Relative phase description string.
     */
    fun formatRelativePhase(group: List<Int>, periods: List<Period>): String {
        if (group.isEmpty()) return ""
        val representativeDay = group.average().toInt()
        val isLutealPhase = representativeDay > 16 || representativeDay < 0

        if (isLutealPhase) {
            val firstDayBefore = abs(group.last())
            val lastDayBefore = abs(group.first())
            return when {
                lastDayBefore == 1 && group.size == 1 -> "on the day before your period"
                group.size == 1 -> "$lastDayBefore days before your period"
                else -> "from $firstDayBefore to $lastDayBefore days before your period"
            }
        } else {
            val avgPeriodLength = periods.mapNotNull {
                it.endDate?.let { endDate -> it.startDate.daysUntil(endDate) }
            }.average().takeIf { !it.isNaN() }?.toInt() ?: 5

            val firstDayAfter = group.first() - avgPeriodLength
            val lastDayAfter = group.last() - avgPeriodLength

            return when {
                firstDayAfter <= 1 && lastDayAfter <= 1 -> "right after your period"
                group.size == 1 -> "$firstDayAfter days after your period"
                else -> "from $firstDayAfter to $lastDayAfter days after your period"
            }
        }
    }
}
