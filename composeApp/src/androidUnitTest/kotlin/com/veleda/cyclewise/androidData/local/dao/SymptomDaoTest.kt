package com.veleda.cyclewise.androidData.local.dao

import com.veleda.cyclewise.KoinTestRule
import com.veleda.cyclewise.androidData.local.database.PeriodDatabase
import com.veleda.cyclewise.androidData.local.entities.SymptomEntity
import com.veleda.cyclewise.domain.models.SymptomCategory
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
class SymptomDaoTest : KoinTest {

    @get:Rule
    val koinRule = KoinTestRule(listOf(testDatabaseModule))

    private val dao: SymptomDao by inject()
    private val db: PeriodDatabase by inject()

    @After
    fun tearDown() {
        db.close()
    }

    // --- Test Data ---
    private val symptomCramps = SymptomEntity("uuid-1", "Cramps", SymptomCategory.PAIN, TestData.INSTANT)
    private val symptomAnxiety = SymptomEntity("uuid-2", "Anxiety", SymptomCategory.MOOD, TestData.INSTANT)
    private val symptomBloating = SymptomEntity("uuid-3", "Bloating", SymptomCategory.DIGESTIVE, TestData.INSTANT)

    // --- Tests for getAllSymptoms() ---

    @Test
    fun getAllSymptoms_WHEN_dataExists_THEN_returnsAllSymptomsSortedByNameAsc() = runTest {
        // ARRANGE
        dao.insert(symptomCramps)
        dao.insert(symptomAnxiety)
        dao.insert(symptomBloating)

        // ACT
        val retrievedList = dao.getAllSymptoms().first()

        // ASSERT
        assertEquals(3, retrievedList.size)
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
        dao.insert(symptomAnxiety)

        // ACT
        val retrievedSymptom = dao.getSymptomByName("anxiety")

        // ASSERT
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
        val originalSymptom = SymptomEntity("uuid-1", "Headache", SymptomCategory.PAIN, TestData.INSTANT)
        dao.insert(originalSymptom)
        val conflictingSymptom = SymptomEntity("uuid-1", "Migraine", SymptomCategory.PAIN, TestData.INSTANT)

        // ACT
        dao.insert(conflictingSymptom)

        // ASSERT
        val retrievedList = dao.getAllSymptoms().first()
        assertEquals(1, retrievedList.size, "Database should still contain only one entry")
        assertEquals("Headache", retrievedList.first().name, "The original name should be preserved")
    }
}
