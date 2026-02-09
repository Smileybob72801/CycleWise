package com.veleda.cyclewise.androidData.local.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.veleda.cyclewise.androidData.local.database.PeriodDatabase
import com.veleda.cyclewise.androidData.local.entities.SymptomEntity
import com.veleda.cyclewise.domain.models.SymptomCategory
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
class SymptomDaoTest : KoinTest {

    // --- SETUP ---
    private val dao: SymptomDao by inject()
    private val db: PeriodDatabase by inject()

    // Define a self-contained Koin module specifically for this test class.
    private val testModule = module {
        single {
            Room.inMemoryDatabaseBuilder(
                ApplicationProvider.getApplicationContext(),
                PeriodDatabase::class.java
            )
                .allowMainThreadQueries()
                .build()
        }
        // Define how to create all DAOs from the database instance.
        single { get<PeriodDatabase>().periodDao() }
        single { get<PeriodDatabase>().dailyEntryDao() }
        single { get<PeriodDatabase>().symptomDao() }
        single { get<PeriodDatabase>().symptomLogDao() }
        single { get<PeriodDatabase>().medicationDao() }
        single { get<PeriodDatabase>().medicationLogDao() }
        single { get<PeriodDatabase>().periodLogDao() }
        single { get<PeriodDatabase>().waterIntakeDao() }
    }

    @Before
    fun setUp() {
        // Start a fresh Koin context for each test using our test-specific module.
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
    private val symptomCramps = SymptomEntity("uuid-1", "Cramps", SymptomCategory.PAIN, Clock.System.now())
    private val symptomAnxiety = SymptomEntity("uuid-2", "Anxiety", SymptomCategory.MOOD, Clock.System.now())
    private val symptomBloating = SymptomEntity("uuid-3", "Bloating", SymptomCategory.DIGESTIVE, Clock.System.now())


    // --- Tests for getAllSymptoms() ---

    @Test
    fun getAllSymptoms_WHEN_dataExists_THEN_returnsAllSymptomsSortedByNameAsc() = runTest {
        // ARRANGE: Insert in a non-alphabetical order to test the sorting
        dao.insert(symptomCramps)
        dao.insert(symptomAnxiety)
        dao.insert(symptomBloating)

        // ACT
        val retrievedList = dao.getAllSymptoms().first()

        // ASSERT
        assertEquals(3, retrievedList.size)
        // Verify the ASC order from the "ORDER BY name ASC" query
        assertEquals("Anxiety", retrievedList[0].name)
        assertEquals("Bloating", retrievedList[1].name)
        assertEquals("Cramps", retrievedList[2].name)
    }

    // --- Tests for getSymptomByName() ---

    @Test
    fun getSymptomByName_WHEN_nameExists_THEN_returnsCorrectSymptom() = runTest {
        // ARRANGE
        dao.insert(symptomCramps)
        dao.insert(symptomAnxiety)

        // ACT
        val retrievedSymptom = dao.getSymptomByName("Anxiety")

        // ASSERT
        assertNotNull(retrievedSymptom)
        assertEquals(symptomAnxiety.id, retrievedSymptom.id)
        assertEquals(symptomAnxiety.name, retrievedSymptom.name)
    }

    @Test
    fun getSymptomByName_WHEN_nameDoesNotExist_THEN_returnsNull() = runTest {
        // ARRANGE
        dao.insert(symptomCramps)

        // ACT
        val retrievedSymptom = dao.getSymptomByName("NonExistentSymptom")

        // ASSERT
        assertNull(retrievedSymptom, "Expected null for a name that is not in the database")
    }

    @Test
    fun getSymptomByName_WHEN_nameIsCaseSensitive_THEN_returnsMatchOnlyOnExactCase() = runTest {
        // ARRANGE
        dao.insert(symptomAnxiety) // Name is "Anxiety"

        // ACT
        val retrievedSymptom = dao.getSymptomByName("anxiety") // Query with lowercase

        // ASSERT
        // Standard SQLite string comparison is case-sensitive by default unless configured otherwise.
        assertNull(retrievedSymptom, "Expected null because the query was case-sensitive")
    }

    // --- Tests for insert() ---

    @Test
    fun insert_WHEN_symptomIsNew_THEN_addsToDatabase() = runTest {
        // ARRANGE
        val initialList = dao.getAllSymptoms().first()
        assertTrue(initialList.isEmpty())

        // ACT
        dao.insert(symptomCramps)

        // ASSERT
        val finalList = dao.getAllSymptoms().first()
        assertEquals(1, finalList.size)
        assertEquals(symptomCramps.name, finalList.first().name)
    }

    @Test
    fun insert_WHEN_symptomWithSameIdExists_THEN_onConflictIgnoreDoesNothing() = runTest {
        // ARRANGE
        // Insert a symptom with a specific name.
        val originalSymptom = SymptomEntity("uuid-1", "Headache", SymptomCategory.PAIN, Clock.System.now())
        dao.insert(originalSymptom)

        // Create a new symptom with the SAME ID but a different name.
        val conflictingSymptom = SymptomEntity("uuid-1", "Migraine", SymptomCategory.PAIN, Clock.System.now())

        // ACT
        // Attempt to insert the conflicting symptom. Due to OnConflictStrategy.IGNORE, this should be a no-op.
        dao.insert(conflictingSymptom)

        // ASSERT
        val retrievedList = dao.getAllSymptoms().first()
        assertEquals(1, retrievedList.size, "Database should still contain only one entry")
        // Verify that the original entry was NOT overwritten
        assertEquals("Headache", retrievedList.first().name, "The original name should be preserved")
    }
}