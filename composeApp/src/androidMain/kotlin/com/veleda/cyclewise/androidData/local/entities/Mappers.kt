package com.veleda.cyclewise.androidData.local.entities

import com.veleda.cyclewise.domain.models.Cycle
import kotlin.time.ExperimentalTime
import com.veleda.cyclewise.domain.models.DailyEntry
import com.veleda.cyclewise.domain.models.FlowIntensity
import com.veleda.cyclewise.domain.models.Medication
import com.veleda.cyclewise.domain.models.Symptom
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString

/** Convert Room entity → shared domain model */
@OptIn(ExperimentalTime::class)
fun CycleEntity.toDomain(): Cycle =
    Cycle(
        id        = uuid,
        startDate = startDate,
        endDate   = endDate,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

/** Convert shared domain model → Room entity (internal id left 0 for autogen) */
@OptIn(ExperimentalTime::class)
fun Cycle.toEntity(): CycleEntity =
    CycleEntity(
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
        cycleId = cycleId,
        entryDate = entryDate,
        dayInCycle = dayInCycle,
        flowIntensity = Converters.toFlowIntensity(flowIntensity),
        moodScore = moodScore,
        energyLevel = energyLevel,
        libidoLevel = Converters.toLibidoLevel(libidoLevel),
        spotting = spotting,
        customTags = Json.decodeFromString(customTags),
        cyclePhase = cyclePhase,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

/** Convert shared domain model → DailyEntryEntity */
@OptIn(ExperimentalTime::class)
fun DailyEntry.toEntity(): DailyEntryEntity =
    DailyEntryEntity(
        id = id,
        cycleId = cycleId,
        entryDate = entryDate,
        dayInCycle = dayInCycle,
        flowIntensity = Converters.fromFlowIntensity(flowIntensity),
        moodScore = moodScore,
        energyLevel = energyLevel,
        libidoLevel = Converters.fromLibidoLevel(libidoLevel),
        spotting = spotting,
        customTags = Json.encodeToString(customTags),
        cyclePhase = cyclePhase,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

// --- Symptom Mappers ---
fun SymptomEntity.toDomain(): Symptom = Symptom(id, entryId, type, severity, note)
fun Symptom.toEntity(): SymptomEntity = SymptomEntity(id, entryId, type, severity, note)


// --- Medication Mappers ---
fun MedicationEntity.toDomain(): Medication = Medication(id, entryId, name, note)
fun Medication.toEntity(): MedicationEntity = MedicationEntity(id, entryId, name, note)