package com.veleda.cyclewise.testutil

import com.benasher44.uuid.uuid4
import com.veleda.cyclewise.androidData.local.entities.*
import com.veleda.cyclewise.domain.models.FlowIntensity
import com.veleda.cyclewise.domain.models.PeriodColor
import com.veleda.cyclewise.domain.models.PeriodConsistency
import com.veleda.cyclewise.domain.models.SymptomCategory
import kotlinx.datetime.LocalDate
import kotlin.time.Instant

/**
 * Factory functions for Room entity construction in tests.
 * All temporal fields default to [TestData.INSTANT] for determinism.
 */

fun buildDailyEntryEntity(
    id: String = "entry-${uuid4()}",
    entryDate: LocalDate = TestData.DATE,
    dayInCycle: Int = 1,
    moodScore: Int? = null,
    energyLevel: Int? = null,
    libidoScore: Int? = null,
    customTags: String = "[]",
    note: String? = null,
    cyclePhase: String? = null,
    createdAt: Instant = TestData.INSTANT,
    updatedAt: Instant = TestData.INSTANT
) = DailyEntryEntity(id, entryDate, dayInCycle, moodScore, energyLevel, libidoScore, customTags, note, cyclePhase, createdAt, updatedAt)

fun buildPeriodEntity(
    id: Int = 0,
    uuid: String = uuid4().toString(),
    startDate: LocalDate = TestData.DATE,
    endDate: LocalDate? = null,
    createdAt: Instant = TestData.INSTANT,
    updatedAt: Instant = TestData.INSTANT
) = PeriodEntity(id, uuid, startDate, endDate, createdAt, updatedAt)

fun buildWaterIntakeEntity(
    date: String = TestData.DATE.toString(),
    cups: Int = 5,
    createdAt: Instant = TestData.INSTANT,
    updatedAt: Instant = TestData.INSTANT
) = WaterIntakeEntity(date, cups, createdAt, updatedAt)

fun buildSymptomEntity(
    id: String = "symptom-${uuid4()}",
    name: String = "TestSymptom",
    category: SymptomCategory = SymptomCategory.PAIN,
    createdAt: Instant = TestData.INSTANT
) = SymptomEntity(id, name, category, createdAt)

fun buildSymptomLogEntity(
    id: String = "slog-${uuid4()}",
    entryId: String = "entry-1",
    symptomId: String = "symptom-1",
    severity: Int = 3,
    createdAt: Instant = TestData.INSTANT
) = SymptomLogEntity(id, entryId, symptomId, severity, createdAt)

fun buildMedicationEntity(
    id: String = "med-${uuid4()}",
    name: String = "TestMed",
    createdAt: Instant = TestData.INSTANT
) = MedicationEntity(id, name, createdAt)

fun buildMedicationLogEntity(
    id: String = "mlog-${uuid4()}",
    entryId: String = "entry-1",
    medicationId: String = "med-1",
    createdAt: Instant = TestData.INSTANT
) = MedicationLogEntity(id, entryId, medicationId, createdAt)

fun buildPeriodLogEntity(
    id: String = "plog-${uuid4()}",
    entryId: String = "entry-1",
    flowIntensity: FlowIntensity = FlowIntensity.MEDIUM,
    periodColor: PeriodColor? = null,
    periodConsistency: PeriodConsistency? = null,
    createdAt: Instant = TestData.INSTANT,
    updatedAt: Instant = TestData.INSTANT
) = PeriodLogEntity(id, entryId, flowIntensity, periodColor, periodConsistency, createdAt, updatedAt)
