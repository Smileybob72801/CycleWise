package com.veleda.cyclewise.androidData.local.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.veleda.cyclewise.androidData.local.database.PeriodDatabase
import com.veleda.cyclewise.androidData.local.entities.MedicationEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.time.Clock
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.inject
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class MedicationDaoTest : KoinTest {

    // --- SETUP ---
    private val dao: MedicationDao by inject()
    private val db: PeriodDatabase by inject()

    private val testModule = module {
        single {
            Room.inMemoryDatabaseBuilder(
                ApplicationProvider.getApplicationContext(),
                PeriodDatabase::class.java
            )
                .allowMainThreadQueries()
                .build()
        }
        // Provide all DAOs
        single { get<PeriodDatabase>().periodDao() }
        single { get<PeriodDatabase>().dailyEntryDao() }
        single { get<PeriodDatabase>().symptomDao() }
        single { get<PeriodDatabase>().symptomLogDao() }
        single { get<PeriodDatabase>().medicationDao() }
        single { get<PeriodDatabase>().medicationLogDao() }
    }

    @Before
    fun setUp() {
        startKoin {
            modules(testModule)
        }
    }

    @After
    fun tearDown() {
        db.close()
        stopKoin()
    }

    // --- Test Data ---
    private val medIbuprofen = MedicationEntity("uuid-1", "Ibuprofen", Clock.System.now())
    private val medAspirin = MedicationEntity("uuid-2", "Aspirin", Clock.System.now())
    private val medTylenol = MedicationEntity("uuid-3", "Tylenol", Clock.System.now())

    // --- Tests for getAllMedications() ---

    @Test
    fun getAllMedications_WHEN_dataExists_THEN_returnsAllMedicationsSortedByNameAsc() = runTest {
        // ARRANGE: Insert in a non-alphabetical order
        dao.insert(medIbuprofen)
        dao.insert(medAspirin)
        dao.insert(medTylenol)

        // ACT
        val retrievedList = dao.getAllMedications().first()

        // ASSERT
        assertEquals(3, retrievedList.size)
        // Verify the ASC order from the "ORDER BY name ASC" query
        assertEquals("Aspirin", retrievedList[0].name)
        assertEquals("Ibuprofen", retrievedList[1].name)
        assertEquals("Tylenol", retrievedList[2].name)
    }

    @Test
    fun getAllMedications_WHEN_databaseIsEmpty_THEN_returnsEmptyList() = runTest {
        val retrievedList = dao.getAllMedications().first()
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
        // This tests the UNIQUE constraint on the `name` column combined with OnConflictStrategy.IGNORE
        // ARRANGE
        val originalMedication = MedicationEntity("uuid-1", "Aspirin", Clock.System.now())
        dao.insert(originalMedication)

        // Create a new medication with a different ID but the SAME name.
        val conflictingMedication = MedicationEntity("uuid-new", "Aspirin", Clock.System.now())

        // ACT
        // Attempt to insert the conflicting medication. Due to the UNIQUE constraint and
        // OnConflictStrategy.IGNORE, this should be a silent no-op.
        dao.insert(conflictingMedication)

        // ASSERT
        val retrievedList = dao.getAllMedications().first()
        assertEquals(1, retrievedList.size, "Database should still contain only one entry")
        // Verify that the original entry's ID was preserved
        assertEquals("uuid-1", retrievedList.first().id, "The original ID should be preserved")
    }
}