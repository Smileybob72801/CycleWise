package com.veleda.cyclewise.domain.insights.analysis

import com.veleda.cyclewise.domain.models.FullDailyLog
import com.veleda.cyclewise.domain.models.Period
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil

/**
 * A completed cycle boundary defined by two consecutive period start dates.
 *
 * @property startDate     Start date of the current period.
 * @property nextStartDate Start date of the following period.
 * @property cycleLength   Number of days between [startDate] and [nextStartDate].
 */
data class CycleBoundary(
    val startDate: LocalDate,
    val nextStartDate: LocalDate,
    val cycleLength: Int,
)

/**
 * A block of consecutive (or near-consecutive) days where a data pattern recurs.
 *
 * @param T             The type identifier for the data being tracked (e.g., symptom ID, mood type).
 * @property dataId          Identifier for the tracked data item.
 * @property normalizedDays  Sorted list of phase-relative day offsets in this block.
 * @property recurrenceCount Minimum occurrence count across the block's days.
 * @property totalCycles     Total number of cycles analyzed.
 * @property recurrenceRate  Fraction of cycles in which this pattern appeared (0.0–1.0).
 */
data class RecurrenceBlock<T>(
    val dataId: T,
    val normalizedDays: List<Int>,
    val recurrenceCount: Int,
    val totalCycles: Int,
    val recurrenceRate: Double,
)

/**
 * Reusable algorithms for cycle-phase pattern analysis.
 *
 * Extracts the shared algorithmic core from [SymptomPhasePatternGenerator] and
 * [MoodPhasePatternGenerator]: cycle boundary construction, day normalization,
 * pattern tallying, significance filtering, and consecutive-day grouping.
 */
object CycleAnalyzer {

    private const val DEFAULT_MAX_CYCLES = 8
    private const val FOLLICULAR_CUTOFF = 16
    private const val DEFAULT_MIN_COUNT = 1
    private const val DEFAULT_MIN_RECURRENCE_RATE = 0.60

    /**
     * Builds a list of [CycleBoundary] from completed periods.
     *
     * Filters to completed periods (non-null [Period.endDate]), reverses to chronological
     * order, pairs consecutive periods via [zipWithNext], and takes the last [maxCycles].
     *
     * @param periods   All periods, sorted by start date descending.
     * @param maxCycles Maximum number of cycle boundaries to return (most recent).
     * @return Chronological cycle boundaries, newest last.
     */
    fun buildCycleBoundaries(
        periods: List<Period>,
        maxCycles: Int = DEFAULT_MAX_CYCLES,
    ): List<CycleBoundary> {
        val chronological = periods.filter { it.endDate != null }.reversed()
        if (chronological.size < 2) return emptyList()
        return chronological.zipWithNext { current, next ->
            CycleBoundary(
                startDate = current.startDate,
                nextStartDate = next.startDate,
                cycleLength = current.startDate.daysUntil(next.startDate),
            )
        }.takeLast(maxCycles)
    }

    /**
     * Normalizes a 1-based day-in-cycle to a phase-relative offset.
     *
     * Days in the follicular phase (1 through [follicularCutoff]) are kept as-is.
     * Days beyond the cutoff are converted to negative luteal-phase offsets relative
     * to the next period start: `day - cycleLength - 1`.
     *
     * @param dayInCycle       1-based day offset from the cycle's start date.
     * @param cycleLength      Total length of the cycle in days.
     * @param follicularCutoff Maximum day number considered follicular (default 16).
     * @return Phase-relative offset: positive for follicular, negative for luteal.
     */
    fun normalizeDay(
        dayInCycle: Int,
        cycleLength: Int,
        follicularCutoff: Int = FOLLICULAR_CUTOFF,
    ): Int = if (dayInCycle <= follicularCutoff) dayInCycle else dayInCycle - cycleLength - 1

    /**
     * Tallies pattern occurrences across cycles using a caller-supplied extractor.
     *
     * For each cycle boundary, filters daily logs to the cycle's date range, then
     * applies [extractor] to each log to extract `(dataId, normalizedDay)` pairs.
     * Returns a map from each unique pair to its occurrence count across all cycles.
     *
     * @param T           The data identifier type (e.g., [String] for symptom IDs).
     * @param boundaries  Cycle boundaries defining the analysis window.
     * @param allLogs     All daily logs across all dates.
     * @param extractor   Function that extracts zero or more `(dataId, normalizedDay)` pairs
     *                    from a single log, given the cycle length for normalization.
     * @return Map from `(dataId, normalizedDay)` to occurrence count.
     */
    fun <T> tallyPatterns(
        boundaries: List<CycleBoundary>,
        allLogs: List<FullDailyLog>,
        extractor: (FullDailyLog, Int) -> List<Pair<T, Int>>,
    ): Map<Pair<T, Int>, Int> {
        val tally = mutableMapOf<Pair<T, Int>, Int>()
        for (boundary in boundaries) {
            val logsForCycle = allLogs.filter {
                it.entry.entryDate >= boundary.startDate && it.entry.entryDate < boundary.nextStartDate
            }
            if (logsForCycle.isEmpty()) continue
            for (log in logsForCycle) {
                for (pair in extractor(log, boundary.cycleLength)) {
                    tally[pair] = (tally[pair] ?: 0) + 1
                }
            }
        }
        return tally
    }

    /**
     * Filters a pattern tally to only statistically significant entries.
     *
     * A pattern is significant if its occurrence count meets [minCount] AND its
     * recurrence rate (count / totalCycles) meets [minRecurrenceRate].
     *
     * @param tally             Map from pattern key to occurrence count.
     * @param totalCycles       Total number of cycles analyzed (denominator for rate).
     * @param minCount          Minimum absolute occurrence count (default 1).
     * @param minRecurrenceRate Minimum fraction of cycles (default 0.60).
     * @return Filtered map containing only significant patterns.
     */
    fun <T> filterSignificant(
        tally: Map<T, Int>,
        totalCycles: Int,
        minCount: Int = DEFAULT_MIN_COUNT,
        minRecurrenceRate: Double = DEFAULT_MIN_RECURRENCE_RATE,
    ): Map<T, Int> = tally.filter { (_, count) ->
        count >= minCount && (count.toDouble() / totalCycles) >= minRecurrenceRate
    }

    /**
     * Groups sorted day offsets into consecutive-day clusters.
     *
     * Two days are in the same group if they differ by at most [maxGap].
     * Symptoms use `maxGap = 1` (strict consecutive); moods use `maxGap = 2`.
     *
     * @param days   Sorted list of normalized day offsets.
     * @param maxGap Maximum allowed gap between consecutive days in a group (default 1).
     * @return List of day groups, each a list of consecutive day offsets.
     */
    fun groupConsecutiveDays(
        days: List<Int>,
        maxGap: Int = 1,
    ): List<List<Int>> {
        if (days.isEmpty()) return emptyList()
        val groups = mutableListOf<MutableList<Int>>()
        groups.add(mutableListOf(days.first()))
        for (i in 1 until days.size) {
            if (days[i] - days[i - 1] <= maxGap) {
                groups.last().add(days[i])
            } else {
                groups.add(mutableListOf(days[i]))
            }
        }
        return groups
    }

    /**
     * Builds recurrence blocks from significant patterns.
     *
     * Groups significant days for each data ID into consecutive-day clusters via
     * [groupConsecutiveDays], then filters to blocks meeting [minBlockSize].
     * Each surviving block is wrapped in a [RecurrenceBlock] with recurrence stats.
     *
     * @param T                  The data identifier type.
     * @param significantPatterns Map from `(dataId, normalizedDay)` to occurrence count.
     * @param totalCycles        Total number of cycles analyzed.
     * @param maxGap             Maximum gap for grouping (see [groupConsecutiveDays]).
     * @param minBlockSize       Minimum number of days per block to retain (default 1).
     * @return List of [RecurrenceBlock]s meeting the size threshold.
     */
    fun <T> buildRecurrenceBlocks(
        significantPatterns: Map<Pair<T, Int>, Int>,
        totalCycles: Int,
        maxGap: Int = 1,
        minBlockSize: Int = 1,
    ): List<RecurrenceBlock<T>> {
        val byDataId = significantPatterns.entries.groupBy({ it.key.first }, { it.key.second })
        val blocks = mutableListOf<RecurrenceBlock<T>>()

        for ((dataId, normalizedDays) in byDataId) {
            val sortedDays = normalizedDays.distinct().sorted()
            val grouped = groupConsecutiveDays(sortedDays, maxGap)

            for (group in grouped) {
                if (group.size < minBlockSize) continue
                val recurrenceCount = group.mapNotNull { day ->
                    significantPatterns[Pair(dataId, day)]
                }.minOrNull() ?: 0
                val recurrenceRate = recurrenceCount.toDouble() / totalCycles

                blocks.add(
                    RecurrenceBlock(
                        dataId = dataId,
                        normalizedDays = group,
                        recurrenceCount = recurrenceCount,
                        totalCycles = totalCycles,
                        recurrenceRate = recurrenceRate,
                    )
                )
            }
        }
        return blocks
    }
}
