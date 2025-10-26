package com.veleda.cyclewise.androidData.local.entities

import com.veleda.cyclewise.domain.models.Period
import kotlin.time.ExperimentalTime
import com.veleda.cyclewise.domain.models.DailyEntry
import com.veleda.cyclewise.domain.models.Medication
import com.veleda.cyclewise.domain.models.MedicationLog
import com.veleda.cyclewise.domain.models.PeriodLog
import com.veleda.cyclewise.domain.models.Symptom
import com.veleda.cyclewise.domain.models.SymptomLog
import kotlinx.serialization.json.Json

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
        id        = 0,           //  Room will auto-generate
        uuid      = id,
        startDate = startDate,
        endDate   = endDate,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

/** Convert DailyEntryEntity → shared domain model */
@OptIn(ExperimentalTime::class)
fun DailyEntryEntity.toDomain(): DailyEntry =
    DailyEntry(
        id = id,
        entryDate = entryDate,
        dayInCycle = dayInCycle,
        moodScore = moodScore,
        energyLevel = energyLevel,
        libidoLevel = Converters.toLibidoLevel(libidoLevel),
        customTags = Json.decodeFromString(customTags),
        note = note,
        cyclePhase = cyclePhase,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

/** Convert shared domain model → DailyEntryEntity */
@OptIn(ExperimentalTime::class)
fun DailyEntry.toEntity(): DailyEntryEntity =
    DailyEntryEntity(
        id = id,
        entryDate = entryDate,
        dayInCycle = dayInCycle,
        moodScore = moodScore,
        energyLevel = energyLevel,
        libidoLevel = Converters.fromLibidoLevel(libidoLevel),
        customTags = Json.encodeToString(customTags),
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

// --- Period Log Mappers ---
@OptIn(ExperimentalTime::class)
fun PeriodLogEntity.toDomain(): PeriodLog = PeriodLog(id, entryId, flowIntensity, createdAt, updatedAt)
@OptIn(ExperimentalTime::class)
fun PeriodLog.toEntity(): PeriodLogEntity = PeriodLogEntity(id, entryId, flowIntensity, createdAt, updatedAt)