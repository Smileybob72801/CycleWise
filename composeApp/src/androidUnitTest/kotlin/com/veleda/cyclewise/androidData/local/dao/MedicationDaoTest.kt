package com.veleda.cyclewise.androidData.local.dao

import com.veleda.cyclewise.KoinTestRule
import com.veleda.cyclewise.androidData.local.database.PeriodDatabase
import com.veleda.cyclewise.androidData.local.entities.MedicationEntity
import com.veleda.cyclewise.testutil.TestData
import com.veleda.cyclewise.testutil.testDatabaseModule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.test.KoinTest
import org.koin.test.inject
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class MedicationDaoTest : KoinTest {

    @get:Rule
    val koinRule = KoinTestRule(listOf(testDatabaseModule))

    private val dao: MedicationDao by inject()
    private val db: PeriodDatabase by inject()

    @After
    fun tearDown() {
        db.close()
    }

    // --- Test Data ---
    private val medIbuprofen = MedicationEntity("uuid-1", "Ibuprofen", TestData.INSTANT)
    private val medAspirin = MedicationEntity("uuid-2", "Aspirin", TestData.INSTANT)
    private val medTylenol = MedicationEntity("uuid-3", "Tylenol", TestData.INSTANT)

    // --- Tests for getAllMedications() ---

    @Test
    fun getAllMedications_WHEN_dataExists_THEN_returnsAllMedicationsSortedByNameAsc() = runTest {
        // ARRANGE
        dao.insert(medIbuprofen)
        dao.insert(medAspirin)
        dao.insert(medTylenol)

        // ACT
        val retrievedList = dao.getAllMedications().first()

        // ASSERT
        assertEquals(3, retrievedList.size)
        assertEquals("Aspirin", retrievedList[0].name)
        assertEquals("Ibuprofen", retrievedList[1].name)
        assertEquals("Tylenol", retrievedList[2].name)
    }

    @Test
    fun getAllMedications_WHEN_databaseIsEmpty_THEN_returnsEmptyList() = runTest {
        // ACT
        val retrievedList = dao.getAllMedications().first()

        // ASSERT
        assertTrue(retrievedList.isEmpty())
    }

    // --- Tests for getMedicationByName() ---

    @Test
    fun getMedicationByName_WHEN_nameExists_THEN_returnsCorrectMedication() = runTest {
        // ARRANGE
        dao.insert(medIbuprofen)
        dao.insert(medAspirin)

        // ACT
        val retrievedMed = dao.getMedicationByName("Ibuprofen")

        // ASSERT
        assertNotNull(retrievedMed)
        assertEquals(medIbuprofen.id, retrievedMed.id)
        assertEquals(medIbuprofen.name, retrievedMed.name)
    }

    @Test
    fun getMedicationByName_WHEN_nameDoesNotExist_THEN_returnsNull() = runTest {
        // ARRANGE
        dao.insert(medAspirin)

        // ACT
        val retrievedMed = dao.getMedicationByName("NonExistentMed")

        // ASSERT
        assertNull(retrievedMed)
    }

    // --- Tests for insert() ---

    @Test
    fun insert_WHEN_medicationIsNew_THEN_addsToDatabase() = runTest {
        // ARRANGE
        assertTrue(dao.getAllMedications().first().isEmpty())

        // ACT
        dao.insert(medAspirin)

        // ASSERT
        val finalList = dao.getAllMedications().first()
        assertEquals(1, finalList.size)
        assertEquals(medAspirin.name, finalList.first().name)
    }

    @Test
    fun insert_WHEN_medicationWithNameExists_THEN_onConflictIgnoreDoesNothing() = runTest {
        // ARRANGE
        val originalMedication = MedicationEntity("uuid-1", "Aspirin", TestData.INSTANT)
        dao.insert(originalMedication)
        val conflictingMedication = MedicationEntity("uuid-new", "Aspirin", TestData.INSTANT)

        // ACT
        dao.insert(conflictingMedication)

        // ASSERT
        val retrievedList = dao.getAllMedications().first()
        assertEquals(1, retrievedList.size, "Database should still contain only one entry")
        assertEquals("uuid-1", retrievedList.first().id, "The original ID should be preserved")
    }
}
