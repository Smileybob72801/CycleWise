package com.veleda.cyclewise.domain.usecases

import com.benasher44.uuid.uuid4
import com.veleda.cyclewise.domain.models.DailyEntry
import com.veleda.cyclewise.domain.models.FlowIntensity
import com.veleda.cyclewise.domain.models.FullDailyLog
import com.veleda.cyclewise.domain.models.MedicationLog
import com.veleda.cyclewise.domain.models.PeriodColor
import com.veleda.cyclewise.domain.models.PeriodConsistency
import com.veleda.cyclewise.domain.models.PeriodLog
import com.veleda.cyclewise.domain.models.SymptomCategory
import com.veleda.cyclewise.domain.models.SymptomLog
import com.veleda.cyclewise.domain.models.WaterIntake
import com.veleda.cyclewise.domain.repository.PeriodRepository
import kotlinx.coroutines.flow.first
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Non-destructive seeder that populates the database with three demo menstrual cycles
 * so new users see a meaningful calendar and insights on first launch.
 *
 * **Guard:** If any periods already exist, [invoke] returns immediately without writing.
 * This prevents duplicate seeding on subsequent launches or after the user has started
 * tracking their own data.
 *
 * Uses existing [PeriodRepository] methods ([createCompletedPeriod], [saveFullLog],
 * [upsertWaterIntake], [createOrGetSymptomInLibrary], [createOrGetMedicationInLibrary])
 * so all domain invariants are preserved.
 *
 * @param repository The data access contract for periods, logs, and libraries.
 * @param clock      Injectable clock for testability; defaults to [Clock.System].
 */
@OptIn(ExperimentalTime::class)
class TutorialSeederUseCase(
    private val repository: PeriodRepository,
    private val clock: Clock = Clock.System,
) {

    /**
     * Seeds three demo cycles if the database is empty.
     *
     * **Cycle layout** (most recent first):
     * - Cycle 1: 5-day period ending ~5 days before today.
     * - Cycle 2: 4-day period starting ~33 days before Cycle 1's start.
     * - Cycle 3: 5-day period starting ~30 days before Cycle 2's start.
     *
     * Each period day gets a [FullDailyLog] with flow, mood, energy, symptoms, and
     * medications. A selection of non-period gap days also receive wellness data.
     * Water intake is seeded for all days that have a daily entry.
     *
     * @return A [SeedManifest] containing all created record IDs, or `null` if
     *         seeding was skipped because periods already exist.
     */
    suspend operator fun invoke(): SeedManifest? {
        // Guard: do not seed if user already has data.
        val existing = repository.getAllPeriods().first()
        if (existing.isNotEmpty()) return null

        val periodUuids = mutableListOf<String>()
        val dailyEntryIds = mutableListOf<String>()
        val waterIntakeDates = mutableListOf<String>()

        val today = clock.todayIn(TimeZone.currentSystemDefault())

        // Pre-create symptom and medication library entries.
        val cramps = repository.createOrGetSymptomInLibrary("Cramps", SymptomCategory.PAIN)
        val bloating = repository.createOrGetSymptomInLibrary("Bloating", SymptomCategory.DIGESTIVE)
        val headache = repository.createOrGetSymptomInLibrary("Headache", SymptomCategory.PAIN)
        val fatigue = repository.createOrGetSymptomInLibrary("Fatigue", SymptomCategory.ENERGY)
        val ibuprofen = repository.createOrGetMedicationInLibrary("Ibuprofen")
        val acetaminophen = repository.createOrGetMedicationInLibrary("Acetaminophen")

        // ── Cycle 1: 5-day period ending 5 days ago ─────────────────────
        val c1End = today.minus(5, DateTimeUnit.DAY)
        val c1Start = c1End.minus(4, DateTimeUnit.DAY) // 5 days inclusive
        val c1Period = repository.createCompletedPeriod(c1Start, c1End)
        periodUuids.add(c1Period.id)

        val c1Flows = listOf(FlowIntensity.MEDIUM, FlowIntensity.HEAVY, FlowIntensity.HEAVY, FlowIntensity.MEDIUM, FlowIntensity.LIGHT)
        val c1Moods = listOf(2, 2, 3, 3, 4)
        val c1Energy = listOf(2, 2, 3, 3, 4)
        val c1Water = listOf(6, 5, 7, 8, 8)

        for (dayOffset in 0..4) {
            val date = c1Start.plus(dayOffset, DateTimeUnit.DAY)
            val entryId = uuid4().toString()
            val now = clock.now()
            val symptomLogs = buildList {
                if (dayOffset in 0..2) add(SymptomLog(uuid4().toString(), entryId, cramps.id, 3, now))
                if (dayOffset in 0..3) add(SymptomLog(uuid4().toString(), entryId, bloating.id, 2, now))
                if (dayOffset == 1) add(SymptomLog(uuid4().toString(), entryId, headache.id, 2, now))
            }
            val medLogs = buildList {
                if (dayOffset in 0..2) add(MedicationLog(uuid4().toString(), entryId, ibuprofen.id, now))
                if (dayOffset == 1) add(MedicationLog(uuid4().toString(), entryId, acetaminophen.id, now))
            }
            val log = FullDailyLog(
                entry = DailyEntry(
                    id = entryId,
                    entryDate = date,
                    dayInCycle = dayOffset + 1,
                    moodScore = c1Moods[dayOffset],
                    energyLevel = c1Energy[dayOffset],
                    note = if (dayOffset == 0) "First day of my period — cramps started early." else null,
                    createdAt = now,
                    updatedAt = now,
                ),
                periodLog = PeriodLog(
                    id = uuid4().toString(),
                    entryId = entryId,
                    flowIntensity = c1Flows[dayOffset],
                    periodColor = PeriodColor.BRIGHT_RED,
                    periodConsistency = PeriodConsistency.MODERATE,
                    createdAt = now,
                    updatedAt = now,
                ),
                symptomLogs = symptomLogs,
                medicationLogs = medLogs,
            )
            repository.saveFullLog(log)
            dailyEntryIds.add(entryId)
            repository.upsertWaterIntake(WaterIntake(date, c1Water[dayOffset], now, now))
            waterIntakeDates.add(date.toString())
        }

        // ── Cycle 2: 4-day period, ~33 days before C1 start ─────────────
        val c2Start = c1Start.minus(33, DateTimeUnit.DAY)
        val c2End = c2Start.plus(3, DateTimeUnit.DAY) // 4 days inclusive
        val c2Period = repository.createCompletedPeriod(c2Start, c2End)
        periodUuids.add(c2Period.id)

        val c2Flows = listOf(FlowIntensity.MEDIUM, FlowIntensity.MEDIUM, FlowIntensity.LIGHT, FlowIntensity.LIGHT)
        val c2Moods = listOf(3, 3, 3, 4)
        val c2Energy = listOf(2, 3, 3, 4)
        val c2Water = listOf(7, 6, 7, 8)

        for (dayOffset in 0..3) {
            val date = c2Start.plus(dayOffset, DateTimeUnit.DAY)
            val entryId = uuid4().toString()
            val now = clock.now()
            val symptomLogs = buildList {
                if (dayOffset in 0..1) add(SymptomLog(uuid4().toString(), entryId, cramps.id, 2, now))
                if (dayOffset in 0..2) add(SymptomLog(uuid4().toString(), entryId, fatigue.id, 2, now))
            }
            val medLogs = buildList {
                if (dayOffset == 0) add(MedicationLog(uuid4().toString(), entryId, ibuprofen.id, now))
            }
            val log = FullDailyLog(
                entry = DailyEntry(
                    id = entryId,
                    entryDate = date,
                    dayInCycle = dayOffset + 1,
                    moodScore = c2Moods[dayOffset],
                    energyLevel = c2Energy[dayOffset],
                    createdAt = now,
                    updatedAt = now,
                ),
                periodLog = PeriodLog(
                    id = uuid4().toString(),
                    entryId = entryId,
                    flowIntensity = c2Flows[dayOffset],
                    periodColor = PeriodColor.DARK_RED,
                    periodConsistency = PeriodConsistency.THIN,
                    createdAt = now,
                    updatedAt = now,
                ),
                symptomLogs = symptomLogs,
                medicationLogs = medLogs,
            )
            repository.saveFullLog(log)
            dailyEntryIds.add(entryId)
            repository.upsertWaterIntake(WaterIntake(date, c2Water[dayOffset], now, now))
            waterIntakeDates.add(date.toString())
        }

        // ── Cycle 3: 5-day period, ~30 days before C2 start ─────────────
        val c3Start = c2Start.minus(30, DateTimeUnit.DAY)
        val c3End = c3Start.plus(4, DateTimeUnit.DAY) // 5 days inclusive
        val c3Period = repository.createCompletedPeriod(c3Start, c3End)
        periodUuids.add(c3Period.id)

        val c3Flows = listOf(FlowIntensity.MEDIUM, FlowIntensity.HEAVY, FlowIntensity.HEAVY, FlowIntensity.MEDIUM, FlowIntensity.LIGHT)
        val c3Moods = listOf(2, 2, 2, 3, 3)
        val c3Energy = listOf(1, 2, 2, 3, 3)
        val c3Water = listOf(5, 5, 6, 7, 8)

        for (dayOffset in 0..4) {
            val date = c3Start.plus(dayOffset, DateTimeUnit.DAY)
            val entryId = uuid4().toString()
            val now = clock.now()
            val symptomLogs = buildList {
                if (dayOffset in 0..3) add(SymptomLog(uuid4().toString(), entryId, cramps.id, 3, now))
                if (dayOffset in 0..2) add(SymptomLog(uuid4().toString(), entryId, bloating.id, 2, now))
                if (dayOffset in 1..2) add(SymptomLog(uuid4().toString(), entryId, headache.id, 3, now))
                add(SymptomLog(uuid4().toString(), entryId, fatigue.id, 2, now)) // all 5 days
            }
            val medLogs = buildList {
                if (dayOffset in 0..3) add(MedicationLog(uuid4().toString(), entryId, ibuprofen.id, now))
            }
            val log = FullDailyLog(
                entry = DailyEntry(
                    id = entryId,
                    entryDate = date,
                    dayInCycle = dayOffset + 1,
                    moodScore = c3Moods[dayOffset],
                    energyLevel = c3Energy[dayOffset],
                    note = if (dayOffset == 2) "Worst cramps today, took extra ibuprofen." else null,
                    createdAt = now,
                    updatedAt = now,
                ),
                periodLog = PeriodLog(
                    id = uuid4().toString(),
                    entryId = entryId,
                    flowIntensity = c3Flows[dayOffset],
                    periodColor = PeriodColor.BRIGHT_RED,
                    periodConsistency = PeriodConsistency.THICK,
                    createdAt = now,
                    updatedAt = now,
                ),
                symptomLogs = symptomLogs,
                medicationLogs = medLogs,
            )
            repository.saveFullLog(log)
            dailyEntryIds.add(entryId)
            repository.upsertWaterIntake(WaterIntake(date, c3Water[dayOffset], now, now))
            waterIntakeDates.add(date.toString())
        }

        // ── Non-period gap days ──────────────────────────────────────────
        // Seed ~60% of gap days with wellness data across all three cycles.
        seedGapDays(c1End, today, headache, fatigue, dailyEntryIds, waterIntakeDates)
        seedGapDays(c2End, c1Start, headache, fatigue, dailyEntryIds, waterIntakeDates)
        seedGapDays(c3End, c2Start, headache, fatigue, dailyEntryIds, waterIntakeDates)

        return SeedManifest(periodUuids, dailyEntryIds, waterIntakeDates)
    }

    /**
     * Seeds a selection of non-period gap days between [gapStart] (exclusive) and
     * [gapEnd] (exclusive) with mood, energy, and occasional symptoms/water.
     *
     * @param dailyEntryIds   Mutable list to append created entry IDs for the manifest.
     * @param waterIntakeDates Mutable list to append created water date strings for the manifest.
     */
    private suspend fun seedGapDays(
        gapStart: LocalDate,
        gapEnd: LocalDate,
        headache: com.veleda.cyclewise.domain.models.Symptom,
        fatigue: com.veleda.cyclewise.domain.models.Symptom,
        dailyEntryIds: MutableList<String>,
        waterIntakeDates: MutableList<String>,
    ) {
        var date = gapStart.plus(1, DateTimeUnit.DAY)
        var dayCount = 0
        while (date < gapEnd) {
            // Seed approximately 60% of gap days.
            if (dayCount % 5 != 2 && dayCount % 5 != 4) {
                val entryId = uuid4().toString()
                val now = clock.now()
                val mood = 3 + (dayCount % 3)  // cycles through 3, 4, 5
                val energy = 3 + ((dayCount + 1) % 3)
                val water = 6 + (dayCount % 3) // cycles through 6, 7, 8

                val symptomLogs = buildList {
                    // Occasional headache or fatigue on some gap days.
                    if (dayCount % 7 == 0) add(SymptomLog(uuid4().toString(), entryId, headache.id, 1, now))
                    if (dayCount % 9 == 0) add(SymptomLog(uuid4().toString(), entryId, fatigue.id, 1, now))
                }

                val note = when {
                    dayCount % 14 == 0 -> "Feeling pretty good today."
                    dayCount % 14 == 7 -> "Busy day, remembered to hydrate."
                    else -> null
                }

                val log = FullDailyLog(
                    entry = DailyEntry(
                        id = entryId,
                        entryDate = date,
                        dayInCycle = 0, // no parent period for gap days
                        moodScore = mood.coerceAtMost(5),
                        energyLevel = energy.coerceAtMost(5),
                        note = note,
                        createdAt = now,
                        updatedAt = now,
                    ),
                    symptomLogs = symptomLogs,
                )
                repository.saveFullLog(log)
                dailyEntryIds.add(entryId)
                repository.upsertWaterIntake(WaterIntake(date, water, now, now))
                waterIntakeDates.add(date.toString())
            }
            date = date.plus(1, DateTimeUnit.DAY)
            dayCount++
        }
    }
}
