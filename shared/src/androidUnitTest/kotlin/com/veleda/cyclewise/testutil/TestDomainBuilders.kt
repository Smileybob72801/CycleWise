package com.veleda.cyclewise.testutil

import com.benasher44.uuid.uuid4
import com.veleda.cyclewise.domain.models.*
import kotlinx.datetime.LocalDate
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * Factory functions for domain model construction in shared module tests.
 * All temporal fields default to [TestData.INSTANT] for determinism.
 */

@OptIn(ExperimentalTime::class)
fun buildPeriod(
    id: String = uuid4().toString(),
    startDate: LocalDate = TestData.DATE,
    endDate: LocalDate? = null,
    createdAt: Instant = TestData.INSTANT,
    updatedAt: Instant = TestData.INSTANT
) = Period(id, startDate, endDate, createdAt, updatedAt)

@OptIn(ExperimentalTime::class)
fun buildDailyEntry(
    id: String = "entry-${uuid4()}",
    entryDate: LocalDate = TestData.DATE,
    dayInCycle: Int = 1,
    moodScore: Int? = null,
    energyLevel: Int? = null,
    libidoScore: Int? = null,
    customTags: List<String> = emptyList(),
    note: String? = null,
    cyclePhase: String? = null,
    createdAt: Instant = TestData.INSTANT,
    updatedAt: Instant = TestData.INSTANT
) = DailyEntry(id, entryDate, dayInCycle, moodScore, energyLevel, libidoScore, customTags, note, cyclePhase, createdAt, updatedAt)

@OptIn(ExperimentalTime::class)
fun buildPeriodLog(
    id: String = uuid4().toString(),
    entryId: String = "entry-1",
    flowIntensity: FlowIntensity = FlowIntensity.MEDIUM,
    periodColor: PeriodColor? = null,
    periodConsistency: PeriodConsistency? = null,
    createdAt: Instant = TestData.INSTANT,
    updatedAt: Instant = TestData.INSTANT
) = PeriodLog(id, entryId, flowIntensity, periodColor, periodConsistency, createdAt, updatedAt)

@OptIn(ExperimentalTime::class)
fun buildSymptom(
    id: String = "symptom-${uuid4()}",
    name: String = "TestSymptom",
    category: SymptomCategory = SymptomCategory.PAIN,
    createdAt: Instant = TestData.INSTANT
) = Symptom(id, name, category, createdAt)

@OptIn(ExperimentalTime::class)
fun buildSymptomLog(
    id: String = "slog-${uuid4()}",
    entryId: String = "entry-1",
    symptomId: String = "symptom-1",
    severity: Int = 3,
    createdAt: Instant = TestData.INSTANT
) = SymptomLog(id, entryId, symptomId, severity, createdAt)

@OptIn(ExperimentalTime::class)
fun buildMedication(
    id: String = "med-${uuid4()}",
    name: String = "TestMed",
    createdAt: Instant = TestData.INSTANT
) = Medication(id, name, createdAt)

@OptIn(ExperimentalTime::class)
fun buildMedicationLog(
    id: String = "mlog-${uuid4()}",
    entryId: String = "entry-1",
    medicationId: String = "med-1",
    createdAt: Instant = TestData.INSTANT
) = MedicationLog(id, entryId, medicationId, createdAt)

@OptIn(ExperimentalTime::class)
fun buildWaterIntake(
    date: LocalDate = TestData.DATE,
    cups: Int = 5,
    createdAt: Instant = TestData.INSTANT,
    updatedAt: Instant = TestData.INSTANT
) = WaterIntake(date, cups, createdAt, updatedAt)

@OptIn(ExperimentalTime::class)
fun buildFullDailyLog(
    entry: DailyEntry = buildDailyEntry(),
    periodLog: PeriodLog? = null,
    symptomLogs: List<SymptomLog> = emptyList(),
    medicationLogs: List<MedicationLog> = emptyList()
) = FullDailyLog(entry, periodLog, symptomLogs, medicationLogs)
