/**
 * Bidirectional mappers between Room entities and shared domain models.
 *
 * Each entity type has a `toDomain()` extension (entity -> domain) and a `toEntity()`
 * extension (domain -> entity). For [PeriodEntity], the internal auto-generated [PeriodEntity.id]
 * is set to 0 on `toEntity()` so Room auto-generates it on insert.
 *
 * [WaterIntakeEntity.date] is converted between ISO-8601 string and [LocalDate].
 */
package com.veleda.cyclewise.androidData.local.entities

import com.veleda.cyclewise.domain.models.CustomTag
import com.veleda.cyclewise.domain.models.CustomTagLog
import com.veleda.cyclewise.domain.models.Period
import kotlin.time.ExperimentalTime
import com.veleda.cyclewise.domain.models.DailyEntry
import com.veleda.cyclewise.domain.models.Medication
import com.veleda.cyclewise.domain.models.MedicationLog
import com.veleda.cyclewise.domain.models.PeriodLog
import com.veleda.cyclewise.domain.models.Symptom
import com.veleda.cyclewise.domain.models.SymptomLog
import com.veleda.cyclewise.domain.models.WaterIntake
import kotlinx.datetime.LocalDate

/** Convert Room entity → shared domain model */
@OptIn(ExperimentalTime::class)
fun PeriodEntity.toDomain(): Period =
    Period(
        id        = uuid,
        startDate = startDate,
        endDate   = endDate,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

/** Convert shared domain model → Room entity (internal id left 0 for autogen) */
@OptIn(ExperimentalTime::class)
fun Period.toEntity(): PeriodEntity =
    PeriodEntity(
        id        = 0,
        uuid      = id,
        startDate = startDate,
        endDate   = endDate,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

/**
 * Converts a [DailyEntryEntity] to the shared [DailyEntry] domain model.
 *
 * The deprecated `custom_tags` JSON column is ignored — custom tags are now stored
 * in the `custom_tag_logs` join table and accessed via [FullDailyLog.customTagLogs].
 */
@OptIn(ExperimentalTime::class)
fun DailyEntryEntity.toDomain(): DailyEntry =
    DailyEntry(
        id = id,
        entryDate = entryDate,
        dayInCycle = dayInCycle,
        moodScore = moodScore,
        energyLevel = energyLevel,
        libidoScore = libidoScore,
        note = note,
        cyclePhase = cyclePhase,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

/**
 * Convert shared domain model → DailyEntryEntity.
 *
 * The deprecated `custom_tags` column is always written as `"[]"` — custom tags
 * are now stored in the `custom_tag_logs` join table.
 */
@OptIn(ExperimentalTime::class)
fun DailyEntry.toEntity(): DailyEntryEntity =
    DailyEntryEntity(
        id = id,
        entryDate = entryDate,
        dayInCycle = dayInCycle,
        moodScore = moodScore,
        energyLevel = energyLevel,
        libidoScore = libidoScore,
        customTags = "[]",
        note = note,
        cyclePhase = cyclePhase,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

// --- Medication Library Mappers ---
fun MedicationEntity.toDomain(): Medication = Medication(id, name, createdAt)
fun Medication.toEntity(): MedicationEntity = MedicationEntity(id, name, createdAt)

// --- Medication Log Mappers ---
fun MedicationLogEntity.toDomain(): MedicationLog = MedicationLog(id, entryId, medicationId, createdAt)
fun MedicationLog.toEntity(): MedicationLogEntity = MedicationLogEntity(id, entryId, medicationId, createdAt)

// --- Symptom Library Mappers ---
fun SymptomEntity.toDomain(): Symptom = Symptom(id, name, category, createdAt)
fun Symptom.toEntity(): SymptomEntity = SymptomEntity(id, name, category, createdAt)

// --- Symptom Log Mappers ---
fun SymptomLogEntity.toDomain(): SymptomLog = SymptomLog(id, entryId, symptomId, severity, createdAt)
fun SymptomLog.toEntity(): SymptomLogEntity = SymptomLogEntity(id, entryId, symptomId, severity, createdAt)

// --- Custom Tag Library Mappers ---
fun CustomTagEntity.toDomain(): CustomTag = CustomTag(id, name, createdAt)
fun CustomTag.toEntity(): CustomTagEntity = CustomTagEntity(id, name, createdAt)

// --- Custom Tag Log Mappers ---
fun CustomTagLogEntity.toDomain(): CustomTagLog = CustomTagLog(id, entryId, tagId, createdAt)
fun CustomTagLog.toEntity(): CustomTagLogEntity = CustomTagLogEntity(id, entryId, tagId, createdAt)

// --- Period Log Mappers ---
@OptIn(ExperimentalTime::class)
fun PeriodLogEntity.toDomain(): PeriodLog = PeriodLog(id, entryId, flowIntensity, periodColor, periodConsistency, createdAt, updatedAt)
@OptIn(ExperimentalTime::class)
fun PeriodLog.toEntity(): PeriodLogEntity = PeriodLogEntity(id, entryId, flowIntensity, periodColor, periodConsistency, createdAt, updatedAt)

// --- Water Intake Mappers ---
@OptIn(ExperimentalTime::class)
fun WaterIntakeEntity.toDomain(): WaterIntake = WaterIntake(LocalDate.parse(date), cups, createdAt, updatedAt)
@OptIn(ExperimentalTime::class)
fun WaterIntake.toEntity(): WaterIntakeEntity = WaterIntakeEntity(date.toString(), cups, createdAt, updatedAt)