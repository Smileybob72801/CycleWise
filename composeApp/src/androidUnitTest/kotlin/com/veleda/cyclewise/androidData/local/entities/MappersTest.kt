package com.veleda.cyclewise.androidData.local.entities

import com.veleda.cyclewise.domain.models.*
import kotlin.time.Clock
import kotlinx.datetime.LocalDate
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for all mapping functions, updated for PeriodLog refactor (v8).
 */
class MappersTest {

    private val testNow = Clock.System.now()
    private val testDate = LocalDate(2025, 8, 18)

    // --- Tests for Period mappers (Unchanged) ---

    @Test
    fun toDomain_WHEN_calledOnCycleEntity_THEN_mapsAllFieldsCorrectly() {
        // ARRANGE
        val periodEntity = PeriodEntity(
            id = 123,
            uuid = "cycle-uuid-1",
            startDate = testDate,
            endDate = testDate,
            createdAt = testNow,
            updatedAt = testNow
        )

        // ACT
        val cycleDomain = periodEntity.toDomain()

        // ASSERT
        assertEquals("cycle-uuid-1", cycleDomain.id)
        assertEquals(testDate, cycleDomain.startDate)
    }

    @Test
    fun toEntity_WHEN_calledOnCycle_THEN_mapsAllFieldsCorrectly() {
        // ARRANGE
        val periodDomain = Period(
            id = "cycle-uuid-1",
            startDate = testDate,
            endDate = testDate,
            createdAt = testNow,
            updatedAt = testNow
        )

        // ACT
        val cycleEntity = periodDomain.toEntity()

        // ASSERT
        assertEquals(0, cycleEntity.id, "Internal DB ID should default to 0 for auto-generation")
        assertEquals("cycle-uuid-1", cycleEntity.uuid)
    }

    // --- Tests for DailyEntry mappers (Updated for refactor) ---

    @Test
    fun toDomain_WHEN_calledOnDailyEntryEntity_THEN_mapsAllFieldsCorrectly() {
        // ARRANGE
        val dailyEntryEntity = DailyEntryEntity(
            id = "entry-uuid-1",
            entryDate = testDate,
            dayInCycle = 5,
            moodScore = 4,
            energyLevel = 3,
            libidoLevel = "HIGH", // String in entity
            customTags = """["tag1","tag2"]""", // JSON String in entity
            note = "Test note",
            cyclePhase = "OVULATION",
            createdAt = testNow,
            updatedAt = testNow
        )

        // ACT
        val dailyEntryDomain = dailyEntryEntity.toDomain()

        // ASSERT
        assertEquals("entry-uuid-1", dailyEntryDomain.id)
        assertEquals(LibidoLevel.HIGH, dailyEntryDomain.libidoLevel, "String should be converted to Enum")
        assertEquals(listOf("tag1", "tag2"), dailyEntryDomain.customTags, "JSON String should be converted to List")
        assertEquals(4, dailyEntryDomain.moodScore)
    }

    @Test
    fun toEntity_WHEN_calledOnDailyEntry_THEN_mapsAllFieldsCorrectly() {
        // ARRANGE
        val dailyEntryDomain = DailyEntry(
            id = "entry-uuid-1",
            entryDate = testDate,
            dayInCycle = 5,
            moodScore = 4,
            energyLevel = 3,
            libidoLevel = LibidoLevel.HIGH, // Enum in domain
            customTags = listOf("tag1", "tag2"), // List in domain
            note = "Test note",
            cyclePhase = "OVULATION",
            createdAt = testNow,
            updatedAt = testNow
        )

        // ACT
        val dailyEntryEntity = dailyEntryDomain.toEntity()

        // ASSERT
        assertEquals("entry-uuid-1", dailyEntryEntity.id)
        // flowIntensity and spotting fields MUST NOT exist here
        assertEquals("HIGH", dailyEntryEntity.libidoLevel, "Enum should be converted to String")
        assertEquals("""["tag1","tag2"]""", dailyEntryEntity.customTags, "List should be converted to JSON String")
        assertEquals(4, dailyEntryEntity.moodScore)
    }

    // --- Tests for PeriodLog mappers (NEW) ---

    @Test
    fun periodLogToDomain_WHEN_calledOnPeriodLogEntity_THEN_mapsAllFieldsCorrectly() {
        // ARRANGE
        val periodLogEntity = PeriodLogEntity(
            id = "plog-uuid",
            entryId = "entry-uuid",
            flowIntensity = FlowIntensity.HEAVY,
            createdAt = testNow,
            updatedAt = testNow
        )

        // ACT
        val periodLogDomain = periodLogEntity.toDomain()

        // ASSERT
        assertEquals("plog-uuid", periodLogDomain.id)
        assertEquals("entry-uuid", periodLogDomain.entryId)
        assertEquals(FlowIntensity.HEAVY, periodLogDomain.flowIntensity)
    }

    @Test
    fun periodLogToEntity_WHEN_calledOnPeriodLog_THEN_mapsAllFieldsCorrectly() {
        // ARRANGE
        val periodLogDomain = PeriodLog(
            id = "plog-uuid",
            entryId = "entry-uuid",
            flowIntensity = FlowIntensity.LIGHT,
            createdAt = testNow,
            updatedAt = testNow
        )

        // ACT
        val periodLogEntity = periodLogDomain.toEntity()

        // ASSERT
        assertEquals("plog-uuid", periodLogEntity.id)
        assertEquals("entry-uuid", periodLogEntity.entryId)
        assertEquals(FlowIntensity.LIGHT, periodLogEntity.flowIntensity)
    }

    // --- Tests for Medication mappers (Unchanged) ---

    @Test
    fun toDomain_WHEN_calledOnMedicationEntity_THEN_mapsAllFieldsCorrectly() {
        val entity = MedicationEntity(id = "med-uuid", name = "Ibuprofen", createdAt = testNow)
        val domain = entity.toDomain()
        assertEquals(entity.id, domain.id)
    }

    // --- Tests for MedicationLog mappers (Unchanged) ---

    @Test
    fun toDomain_WHEN_calledOnMedicationLogEntity_THEN_mapsAllFieldsCorrectly() {
        val entity = MedicationLogEntity(id = "log-uuid", entryId = "entry-uuid", medicationId = "med-uuid", createdAt = testNow)
        val domain = entity.toDomain()
        assertEquals(entity.id, domain.id)
    }

    // --- Tests for Symptom mappers (Unchanged) ---

    @Test
    fun toDomain_WHEN_calledOnSymptomEntity_THEN_mapsAllFieldsCorrectly() {
        val entity = SymptomEntity(id = "symptom-uuid", name = "Cramps", category = SymptomCategory.PAIN, createdAt = testNow)
        val domain = entity.toDomain()
        assertEquals(entity.id, domain.id)
    }

    // --- Tests for SymptomLog mappers (Unchanged) ---

    @Test
    fun toDomain_WHEN_calledOnSymptomLogEntity_THEN_mapsAllFieldsCorrectly() {
        val entity = SymptomLogEntity(id = "log-uuid", entryId = "entry-uuid", symptomId = "symptom-uuid", severity = 4, createdAt = testNow)
        val domain = entity.toDomain()
        assertEquals(entity.id, domain.id)
    }
}