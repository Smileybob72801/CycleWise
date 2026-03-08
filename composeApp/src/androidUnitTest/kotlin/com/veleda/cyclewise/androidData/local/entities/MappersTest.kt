package com.veleda.cyclewise.androidData.local.entities

import com.veleda.cyclewise.domain.models.*
import com.veleda.cyclewise.testutil.TestData
import kotlinx.datetime.LocalDate
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MappersTest {

    private val testNow = TestData.INSTANT
    private val testDate = LocalDate(2025, 8, 18)

    // --- Tests for Period mappers ---

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

    // --- Tests for DailyEntry mappers ---

    @Test
    fun toDomain_WHEN_calledOnDailyEntryEntity_THEN_mapsAllFieldsCorrectly() {
        // ARRANGE
        val dailyEntryEntity = DailyEntryEntity(
            id = "entry-uuid-1",
            entryDate = testDate,
            dayInCycle = 5,
            moodScore = 4,
            energyLevel = 3,
            libidoScore = 4,
            customTags = """["tag1","tag2"]""",
            note = "Test note",
            cyclePhase = "OVULATION",
            createdAt = testNow,
            updatedAt = testNow
        )

        // ACT
        val dailyEntryDomain = dailyEntryEntity.toDomain()

        // ASSERT
        assertEquals("entry-uuid-1", dailyEntryDomain.id)
        assertEquals(4, dailyEntryDomain.libidoScore, "Integer should map directly")
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
            libidoScore = 4,
            customTags = listOf("tag1", "tag2"),
            note = "Test note",
            cyclePhase = "OVULATION",
            createdAt = testNow,
            updatedAt = testNow
        )

        // ACT
        val dailyEntryEntity = dailyEntryDomain.toEntity()

        // ASSERT
        assertEquals("entry-uuid-1", dailyEntryEntity.id)
        assertEquals(4, dailyEntryEntity.libidoScore, "Integer should map directly")
        assertEquals("""["tag1","tag2"]""", dailyEntryEntity.customTags, "List should be converted to JSON String")
        assertEquals(4, dailyEntryEntity.moodScore)
    }

    @Test
    fun toDomain_WHEN_dailyEntryHasNullOptionals_THEN_mapsNullsCorrectly() {
        // ARRANGE
        val entity = DailyEntryEntity(
            id = "entry-null-test",
            entryDate = testDate,
            dayInCycle = 1,
            moodScore = null,
            energyLevel = null,
            libidoScore = null,
            customTags = "[]",
            note = null,
            cyclePhase = null,
            createdAt = testNow,
            updatedAt = testNow
        )

        // ACT
        val domain = entity.toDomain()

        // ASSERT
        assertNull(domain.moodScore)
        assertNull(domain.energyLevel)
        assertNull(domain.libidoScore)
        assertNull(domain.note)
        assertNull(domain.cyclePhase)
    }

    @Test
    fun toDomain_WHEN_dailyEntryHasEmptyCustomTags_THEN_mapsToEmptyList() {
        // ARRANGE
        val entity = DailyEntryEntity(
            id = "entry-empty-tags",
            entryDate = testDate,
            dayInCycle = 1,
            customTags = "[]",
            createdAt = testNow,
            updatedAt = testNow
        )

        // ACT
        val domain = entity.toDomain()

        // ASSERT
        assertTrue(domain.customTags.isEmpty())
    }

    @Test
    fun toEntity_WHEN_dailyEntryHasEmptyTagsList_THEN_mapsToEmptyJsonArray() {
        // ARRANGE
        val domain = DailyEntry(
            id = "entry-empty-tags",
            entryDate = testDate,
            dayInCycle = 1,
            customTags = emptyList(),
            createdAt = testNow,
            updatedAt = testNow
        )

        // ACT
        val entity = domain.toEntity()

        // ASSERT
        assertEquals("[]", entity.customTags)
    }

    // --- Tests for PeriodLog mappers ---

    @Test
    fun periodLogToDomain_WHEN_calledOnPeriodLogEntity_THEN_mapsAllFieldsCorrectly() {
        // ARRANGE
        val periodLogEntity = PeriodLogEntity(
            id = "plog-uuid",
            entryId = "entry-uuid",
            flowIntensity = FlowIntensity.HEAVY,
            periodColor = PeriodColor.DARK_RED,
            periodConsistency = PeriodConsistency.CLOTS_SMALL,
            createdAt = testNow,
            updatedAt = testNow
        )

        // ACT
        val periodLogDomain = periodLogEntity.toDomain()

        // ASSERT
        assertEquals("plog-uuid", periodLogDomain.id)
        assertEquals("entry-uuid", periodLogDomain.entryId)
        assertEquals(FlowIntensity.HEAVY, periodLogDomain.flowIntensity)
        assertEquals(PeriodColor.DARK_RED, periodLogDomain.periodColor)
        assertEquals(PeriodConsistency.CLOTS_SMALL, periodLogDomain.periodConsistency)
    }

    @Test
    fun periodLogToEntity_WHEN_calledOnPeriodLog_THEN_mapsAllFieldsCorrectly() {
        // ARRANGE
        val periodLogDomain = PeriodLog(
            id = "plog-uuid",
            entryId = "entry-uuid",
            flowIntensity = FlowIntensity.LIGHT,
            periodColor = PeriodColor.PINK,
            periodConsistency = PeriodConsistency.THIN,
            createdAt = testNow,
            updatedAt = testNow
        )

        // ACT
        val periodLogEntity = periodLogDomain.toEntity()

        // ASSERT
        assertEquals("plog-uuid", periodLogEntity.id)
        assertEquals("entry-uuid", periodLogEntity.entryId)
        assertEquals(FlowIntensity.LIGHT, periodLogEntity.flowIntensity)
        assertEquals(PeriodColor.PINK, periodLogEntity.periodColor)
        assertEquals(PeriodConsistency.THIN, periodLogEntity.periodConsistency)
    }

    @Test
    fun periodLogToDomain_WHEN_colorAndConsistencyAreNull_THEN_mapsNullsCorrectly() {
        // ARRANGE
        val periodLogEntity = PeriodLogEntity(
            id = "plog-null-test",
            entryId = "entry-uuid",
            flowIntensity = FlowIntensity.MEDIUM,
            periodColor = null,
            periodConsistency = null,
            createdAt = testNow,
            updatedAt = testNow
        )

        // ACT
        val periodLogDomain = periodLogEntity.toDomain()

        // ASSERT
        assertNull(periodLogDomain.periodColor)
        assertNull(periodLogDomain.periodConsistency)
    }

    @Test
    fun periodLogToDomain_WHEN_flowIntensityNull_THEN_mapsNullCorrectly() {
        // ARRANGE
        val periodLogEntity = PeriodLogEntity(
            id = "plog-null-flow",
            entryId = "entry-uuid",
            flowIntensity = null,
            periodColor = PeriodColor.BRIGHT_RED,
            periodConsistency = PeriodConsistency.THIN,
            createdAt = testNow,
            updatedAt = testNow
        )

        // ACT
        val domain = periodLogEntity.toDomain()
        val roundTripped = domain.toEntity()

        // ASSERT — null flowIntensity round-trips correctly
        assertNull(domain.flowIntensity)
        assertEquals(PeriodColor.BRIGHT_RED, domain.periodColor)
        assertNull(roundTripped.flowIntensity)
        assertEquals("plog-null-flow", roundTripped.id)
    }

    // --- Tests for Medication mappers ---

    @Test
    fun toDomain_WHEN_calledOnMedicationEntity_THEN_mapsAllFieldsCorrectly() {
        // ARRANGE
        val entity = MedicationEntity(id = "med-uuid", name = "Ibuprofen", createdAt = testNow)

        // ACT
        val domain = entity.toDomain()

        // ASSERT
        assertEquals(entity.id, domain.id)
    }

    // --- Tests for MedicationLog mappers ---

    @Test
    fun toDomain_WHEN_calledOnMedicationLogEntity_THEN_mapsAllFieldsCorrectly() {
        // ARRANGE
        val entity = MedicationLogEntity(id = "log-uuid", entryId = "entry-uuid", medicationId = "med-uuid", createdAt = testNow)

        // ACT
        val domain = entity.toDomain()

        // ASSERT
        assertEquals(entity.id, domain.id)
    }

    // --- Tests for Symptom mappers ---

    @Test
    fun toDomain_WHEN_calledOnSymptomEntity_THEN_mapsAllFieldsCorrectly() {
        // ARRANGE
        val entity = SymptomEntity(id = "symptom-uuid", name = "Cramps", category = SymptomCategory.PAIN, createdAt = testNow)

        // ACT
        val domain = entity.toDomain()

        // ASSERT
        assertEquals(entity.id, domain.id)
    }

    // --- Tests for SymptomLog mappers ---

    @Test
    fun toDomain_WHEN_calledOnSymptomLogEntity_THEN_mapsAllFieldsCorrectly() {
        // ARRANGE
        val entity = SymptomLogEntity(id = "log-uuid", entryId = "entry-uuid", symptomId = "symptom-uuid", severity = 4, createdAt = testNow)

        // ACT
        val domain = entity.toDomain()

        // ASSERT
        assertEquals(entity.id, domain.id)
    }
}
