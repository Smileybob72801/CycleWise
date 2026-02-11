package com.veleda.cyclewise.testutil

import com.benasher44.uuid.uuid4
import com.veleda.cyclewise.domain.models.*
import kotlinx.datetime.LocalDate
import kotlin.time.Instant

/**
 * Factory functions for domain model construction in composeApp tests.
 * All temporal fields default to [TestData.INSTANT] for determinism.
 */

fun buildPeriod(
    id: String = uuid4().toString(),
    startDate: LocalDate = TestData.DATE,
    endDate: LocalDate? = null,
    createdAt: Instant = TestData.INSTANT,
    updatedAt: Instant = TestData.INSTANT
) = Period(id, startDate, endDate, createdAt, updatedAt)

fun buildDailyEntry(
    id: String = "entry-${uuid4()}",
    entryDate: LocalDate = TestData.DATE,
    dayInCycle: Int = 1,
    moodScore: Int? = null,
    energyLevel: Int? = null,
    libidoLevel: LibidoLevel? = null,
    customTags: List<String> = emptyList(),
    note: String? = null,
    cyclePhase: String? = null,
    createdAt: Instant = TestData.INSTANT,
    updatedAt: Instant = TestData.INSTANT
) = DailyEntry(id, entryDate, dayInCycle, moodScore, energyLevel, libidoLevel, customTags, note, cyclePhase, createdAt, updatedAt)

fun buildPeriodLog(
    id: String = uuid4().toString(),
    entryId: String = "entry-1",
    flowIntensity: FlowIntensity = FlowIntensity.MEDIUM,
    createdAt: Instant = TestData.INSTANT,
    updatedAt: Instant = TestData.INSTANT
) = PeriodLog(id, entryId, flowIntensity, createdAt, updatedAt)

fun buildSymptom(
    id: String = "symptom-${uuid4()}",
    name: String = "TestSymptom",
    category: SymptomCategory = SymptomCategory.PAIN,
    createdAt: Instant = TestData.INSTANT
) = Symptom(id, name, category, createdAt)

fun buildSymptomLog(
    id: String = "slog-${uuid4()}",
    entryId: String = "entry-1",
    symptomId: String = "symptom-1",
    severity: Int = 3,
    createdAt: Instant = TestData.INSTANT
) = SymptomLog(id, entryId, symptomId, severity, createdAt)

fun buildMedication(
    id: String = "med-${uuid4()}",
    name: String = "TestMed",
    createdAt: Instant = TestData.INSTANT
) = Medication(id, name, createdAt)

fun buildMedicationLog(
    id: String = "mlog-${uuid4()}",
    entryId: String = "entry-1",
    medicationId: String = "med-1",
    createdAt: Instant = TestData.INSTANT
) = MedicationLog(id, entryId, medicationId, createdAt)

fun buildWaterIntake(
    date: LocalDate = TestData.DATE,
    cups: Int = 5,
    createdAt: Instant = TestData.INSTANT,
    updatedAt: Instant = TestData.INSTANT
) = WaterIntake(date, cups, createdAt, updatedAt)

fun buildFullDailyLog(
    entry: DailyEntry = buildDailyEntry(),
    periodLog: PeriodLog? = null,
    symptomLogs: List<SymptomLog> = emptyList(),
    medicationLogs: List<MedicationLog> = emptyList()
) = FullDailyLog(entry, periodLog, symptomLogs, medicationLogs)
