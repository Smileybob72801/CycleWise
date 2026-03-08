package com.veleda.cyclewise.domain

import com.veleda.cyclewise.domain.models.CyclePhase
import com.veleda.cyclewise.domain.models.Period
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil

/**
 * Stateless calculator that determines the [CyclePhase] for any given date
 * based on period history and average cycle length.
 *
 * Phase boundaries follow a standard ovulatory model where the luteal phase
 * is fixed at 14 days and the ovulation window spans 3 days before that.
 *
 * All functions are pure — no side effects, no state, no I/O.
 */
object CyclePhaseCalculator {

    /** Fixed luteal phase length in days (standard ovulatory model). */
    private const val LUTEAL_PHASE_LENGTH = 14

    /** Number of days in the ovulation window preceding the luteal phase. */
    private const val OVULATION_WINDOW_LENGTH = 3

    /**
     * Determines the cycle phase for [date] given the user's [periods] and [averageCycleLength].
     *
     * Algorithm:
     * 1. Sort periods chronologically by start date.
     * 2. Find the **owning period** — the period whose start date is on or before [date]
     *    and whose next period's start date is after [date] (or it is the most recent period).
     * 3. If [date] falls within the owning period's start..end range, return [CyclePhase.MENSTRUATION].
     * 4. Determine expected cycle length: actual (start-to-start) for past cycles,
     *    or [averageCycleLength] for the current/latest cycle.
     * 5. Compute phase boundaries forward from the owning period's start date.
     *
     * @param date               the date to classify.
     * @param periods            all recorded periods (any sort order accepted).
     * @param averageCycleLength average cycle length in days, or null if insufficient data.
     * @return the computed [CyclePhase], or null if the date cannot be classified.
     */
    fun calculatePhase(
        date: LocalDate,
        periods: List<Period>,
        averageCycleLength: Double?
    ): CyclePhase? {
        if (periods.isEmpty()) return null

        val sorted = periods.sortedBy { it.startDate }

        // Date is before any recorded period
        if (date < sorted.first().startDate) return null

        // Find the owning period: last period whose startDate <= date
        val owningIndex = sorted.indexOfLast { it.startDate <= date }
        if (owningIndex == -1) return null

        val owningPeriod = sorted[owningIndex]
        val nextPeriod = sorted.getOrNull(owningIndex + 1)

        // If there's a next period and date is on or after its start, the date
        // belongs to that cycle instead — recurse is unnecessary since we used indexOfLast
        if (nextPeriod != null && date >= nextPeriod.startDate) return null

        // Check if date is within the menstruation range
        val periodEnd = owningPeriod.endDate
        if (periodEnd != null && date in owningPeriod.startDate..periodEnd) {
            return CyclePhase.MENSTRUATION
        }
        // Ongoing period with no end date: all days from start onward are menstruation
        if (periodEnd == null && date >= owningPeriod.startDate && nextPeriod == null) {
            // For ongoing periods, if we have no end date, days from start are menstruation
            // unless we have an average and can predict the end
            return CyclePhase.MENSTRUATION
        }

        // Determine cycle length for phase computation
        val cycleLength: Int = if (nextPeriod != null) {
            // Past cycle: actual start-to-start distance
            owningPeriod.startDate.daysUntil(nextPeriod.startDate)
        } else {
            // Current/latest cycle: use average
            averageCycleLength?.toInt() ?: return null
        }

        // Guard: cycle too short for meaningful phase computation
        if (cycleLength < 1) return null

        val dayInCycle = owningPeriod.startDate.daysUntil(date) + 1 // 1-based

        // Menstruation end day (1-based) — use actual period end if available
        val menstruationEndDay = if (periodEnd != null) {
            owningPeriod.startDate.daysUntil(periodEnd) + 1
        } else {
            // Shouldn't reach here for ongoing with no next period (handled above),
            // but for safety:
            1
        }

        // Luteal starts at cycleLength - LUTEAL_PHASE_LENGTH + 1 (1-based)
        val lutealStartDay = cycleLength - LUTEAL_PHASE_LENGTH + 1

        // Ovulation window: 3 days before luteal start
        val ovulationStartDay = lutealStartDay - OVULATION_WINDOW_LENGTH
        val ovulationEndDay = lutealStartDay - 1

        return when {
            dayInCycle <= menstruationEndDay -> CyclePhase.MENSTRUATION
            dayInCycle > cycleLength -> null // Past expected cycle end
            dayInCycle >= lutealStartDay -> CyclePhase.LUTEAL
            dayInCycle in ovulationStartDay..ovulationEndDay -> CyclePhase.OVULATION
            dayInCycle > menstruationEndDay -> CyclePhase.FOLLICULAR
            else -> null
        }
    }

    /**
     * Computes the average cycle length (start-to-start) from completed periods.
     *
     * Only periods with a non-null [Period.endDate] are considered. At least 2 completed
     * periods are required to compute a meaningful average.
     *
     * @param periods all recorded periods (any sort order accepted).
     * @return the average cycle length in days, or null if fewer than 2 completed periods exist.
     */
    fun averageCycleLength(periods: List<Period>): Double? {
        val completed = periods.filter { it.endDate != null }.sortedBy { it.startDate }
        if (completed.size < 2) return null
        return completed.zipWithNext { current, next ->
            current.startDate.daysUntil(next.startDate).toDouble()
        }.average()
    }
}
