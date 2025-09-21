package com.veleda.cyclewise.androidData.local.entities

import com.veleda.cyclewise.domain.models.*
import kotlin.time.Clock
import kotlinx.datetime.LocalDate
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Unit tests for all mapping functions.
 */
class MappersTest {

    private val testNow = Clock.System.now()
    private val testDate = LocalDate(2025, 8, 18)

    // --- Tests for Cycle mappers ---

    @Test
    fun toDomain_WHEN_calledOnCycleEntity_THEN_mapsAllFieldsCorrectly() {
        // ARRANGE
        val cycleEntity = CycleEntity(
            id = 123,
            uuid = "cycle-uuid-1",
            startDate = testDate,
            endDate = testDate,
            createdAt = testNow,
            updatedAt = testNow
        )

        // ACT
        val cycleDomain = cycleEntity.toDomain()

        // ASSERT
        assertEquals("cycle-uuid-1", cycleDomain.id)
        assertEquals(testDate, cycleDomain.startDate)
        assertEquals(testDate, cycleDomain.endDate)
        assertEquals(testNow, cycleDomain.createdAt)
        assertEquals(testNow, cycleDomain.updatedAt)
    }

    @Test
    fun toEntity_WHEN_calledOnCycle_THEN_mapsAllFieldsCorrectly() {
        // ARRANGE
        val cycleDomain = Cycle(
            id = "cycle-uuid-1",
            startDate = testDate,
            endDate = testDate,
            createdAt = testNow,
            updatedAt = testNow
        )

        // ACT
        val cycleEntity = cycleDomain.toEntity()

        // ASSERT
        assertEquals(0, cycleEntity.id, "Internal DB ID should default to 0 for auto-generation")
        assertEquals("cycle-uuid-1", cycleEntity.uuid)
        assertEquals(testDate, cycleEntity.startDate)
        assertEquals(testDate, cycleEntity.endDate)
        assertEquals(testNow, cycleEntity.createdAt)
        assertEquals(testNow, cycleEntity.updatedAt)
    }

    // --- Tests for DailyEntry mappers ---

    @Test
    fun toDomain_WHEN_calledOnDailyEntryEntity_THEN_mapsAllFieldsAndConvertsTypes() {
        // ARRANGE
        val dailyEntryEntity = DailyEntryEntity(
            id = "entry-uuid-1",
            cycleId = "cycle-uuid-1",
            entryDate = testDate,
            dayInCycle = 5,
            flowIntensity = "HEAVY", // String in entity
            moodScore = 4,
            libidoLevel = "HIGH", // String in entity
            spotting = true,
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
        assertEquals("cycle-uuid-1", dailyEntryDomain.cycleId)
        assertEquals(FlowIntensity.HEAVY, dailyEntryDomain.flowIntensity, "String should be converted to Enum")
        assertEquals(LibidoLevel.HIGH, dailyEntryDomain.libidoLevel, "String should be converted to Enum")
        assertEquals(listOf("tag1", "tag2"), dailyEntryDomain.customTags, "JSON String should be converted to List")
        assertEquals("Test note", dailyEntryDomain.note)
        assertTrue(dailyEntryDomain.spotting)
    }

    @Test
    fun toEntity_WHEN_calledOnDailyEntry_THEN_mapsAllFieldsAndConvertsTypes() {
        // ARRANGE
        val dailyEntryDomain = DailyEntry(
            id = "entry-uuid-1",
            cycleId = "cycle-uuid-1",
            entryDate = testDate,
            dayInCycle = 5,
            flowIntensity = FlowIntensity.HEAVY, // Enum in domain
            moodScore = 4,
            libidoLevel = LibidoLevel.HIGH, // Enum in domain
            spotting = true,
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
        assertEquals("HEAVY", dailyEntryEntity.flowIntensity, "Enum should be converted to String")
        assertEquals("HIGH", dailyEntryEntity.libidoLevel, "Enum should be converted to String")
        assertEquals("""["tag1","tag2"]""", dailyEntryEntity.customTags, "List should be converted to JSON String")
    }

    // --- Tests for Medication mappers ---

    @Test
    fun toDomain_WHEN_calledOnMedicationEntity_THEN_mapsAllFieldsCorrectly() {
        val entity = MedicationEntity(id = "med-uuid", name = "Ibuprofen", createdAt = testNow)
        val domain = entity.toDomain()
        assertEquals(entity.id, domain.id)
        assertEquals(entity.name, domain.name)
        assertEquals(entity.createdAt, domain.createdAt)
    }

    @Test
    fun toEntity_WHEN_calledOnMedication_THEN_mapsAllFieldsCorrectly() {
        val domain = Medication(id = "med-uuid", name = "Ibuprofen", createdAt = testNow)
        val entity = domain.toEntity()
        assertEquals(domain.id, entity.id)
        assertEquals(domain.name, entity.name)
        assertEquals(domain.createdAt, entity.createdAt)
    }

    // --- Tests for MedicationLog mappers ---

    @Test
    fun toDomain_WHEN_calledOnMedicationLogEntity_THEN_mapsAllFieldsCorrectly() {
        val entity = MedicationLogEntity(id = "log-uuid", entryId = "entry-uuid", medicationId = "med-uuid", createdAt = testNow)
        val domain = entity.toDomain()
        assertEquals(entity.id, domain.id)
        assertEquals(entity.entryId, domain.entryId)
        assertEquals(entity.medicationId, domain.medicationId)
        assertEquals(entity.createdAt, domain.createdAt)
    }

    // --- Tests for Symptom mappers ---

    @Test
    fun toDomain_WHEN_calledOnSymptomEntity_THEN_mapsAllFieldsCorrectly() {
        val entity = SymptomEntity(id = "symptom-uuid", name = "Cramps", category = SymptomCategory.PAIN, createdAt = testNow)
        val domain = entity.toDomain()
        assertEquals(entity.id, domain.id)
        assertEquals(entity.name, domain.name)
        assertEquals(entity.category, domain.category)
        assertEquals(entity.createdAt, domain.createdAt)
    }

    // --- Tests for SymptomLog mappers ---

    @Test
    fun toDomain_WHEN_calledOnSymptomLogEntity_THEN_mapsAllFieldsCorrectly() {
        val entity = SymptomLogEntity(id = "log-uuid", entryId = "entry-uuid", symptomId = "symptom-uuid", severity = 4, createdAt = testNow)
        val domain = entity.toDomain()
        assertEquals(entity.id, domain.id)
        assertEquals(entity.entryId, domain.entryId)
        assertEquals(entity.symptomId, domain.symptomId)
        assertEquals(entity.severity, domain.severity)
        assertEquals(entity.createdAt, domain.createdAt)
    }
}