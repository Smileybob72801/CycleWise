package com.veleda.cyclewise.androidData.local.dao

import com.veleda.cyclewise.KoinTestRule
import com.veleda.cyclewise.androidData.local.database.PeriodDatabase
import com.veleda.cyclewise.androidData.local.entities.DailyEntryEntity
import com.veleda.cyclewise.testutil.TestData
import com.veleda.cyclewise.testutil.testDatabaseModule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.test.KoinTest
import org.koin.test.inject
import org.robolectric.RobolectricTestRunner
import kotlin.test.*

@RunWith(RobolectricTestRunner::class)
class DailyEntryDaoTest : KoinTest {

    @get:Rule
    val koinRule = KoinTestRule(listOf(testDatabaseModule))

    private val dao: DailyEntryDao by inject()
    private val db: PeriodDatabase by inject()

    @After
    fun tearDown() {
        db.close()
    }

    // --- Test Data ---
    private val entry1 = DailyEntryEntity(
        id = "entry-uuid-1",
        entryDate = LocalDate(2025, 1, 10),
        dayInCycle = 10,
        customTags = "[]",
        createdAt = TestData.INSTANT, updatedAt = TestData.INSTANT
    )
    private val entry2 = DailyEntryEntity(
        id = "entry-uuid-2",
        entryDate = LocalDate(2025, 1, 12),
        dayInCycle = 12,
        moodScore = 4,
        customTags = "[]",
        createdAt = TestData.INSTANT, updatedAt = TestData.INSTANT
    )
    private val entry3 = DailyEntryEntity(
        id = "entry-uuid-3",
        entryDate = LocalDate(2025, 1, 11),
        dayInCycle = 11,
        customTags = "[]",
        createdAt = TestData.INSTANT, updatedAt = TestData.INSTANT
    )

    // --- Tests for insert() and update() ---

    @Test
    fun insert_WHEN_entryIsNew_THEN_addsToDatabase() = runTest {
        // ACT
        dao.insert(entry1)

        // ASSERT
        val retrieved = dao.getEntryForDate(entry1.entryDate).first()
        assertNotNull(retrieved)
        assertEquals(entry1.id, retrieved.id)
    }

    @Test
    fun insert_WHEN_entryWithSameIdExists_THEN_onConflictReplaceUpdatesTheRecord() = runTest {
        // ARRANGE
        dao.insert(entry1)
        val updatedEntry = entry1.copy(moodScore = 5)

        // ACT
        dao.insert(updatedEntry)

        // ASSERT
        val retrieved = dao.getEntryForDate(entry1.entryDate).first()
        assertNotNull(retrieved)
        assertEquals(1, dao.getAllEntries().first().size)
        assertEquals(5, retrieved.moodScore)
    }

    @Test
    fun update_WHEN_entryExists_THEN_modifiesTheRecord() = runTest {
        // ARRANGE
        dao.insert(entry2)

        // ACT
        val modifiedEntry = entry2.copy(moodScore = 2)
        dao.update(modifiedEntry)

        // ASSERT
        val retrieved = dao.getEntryForDate(entry2.entryDate).first()
        assertNotNull(retrieved)
        assertEquals(2, retrieved.moodScore)
    }

    // --- Tests for getEntryForDate() ---

    @Test
    fun getEntryForDate_WHEN_entryExists_THEN_returnsCorrectEntry() = runTest {
        // ARRANGE
        dao.insert(entry1)

        // ACT
        val retrieved = dao.getEntryForDate(LocalDate(2025, 1, 10)).first()

        // ASSERT
        assertNotNull(retrieved)
        assertEquals(entry1.id, retrieved.id)
    }

    @Test
    fun getEntryForDate_WHEN_entryDoesNotExist_THEN_returnsNull() = runTest {
        // ACT
        val retrieved = dao.getEntryForDate(LocalDate(2025, 1, 10)).first()

        // ASSERT
        assertNull(retrieved)
    }

    // --- Tests for getEntriesForDateRange() ---

    @Test
    fun getEntriesForDateRange_WHEN_rangeIsInclusive_THEN_returnsCorrectEntries() = runTest {
        // ARRANGE
        dao.insert(entry1) // Jan 10
        dao.insert(entry2) // Jan 12
        dao.insert(entry3) // Jan 11

        // ACT
        val retrievedList = dao.getEntriesForDateRange(
            startDate = LocalDate(2025, 1, 11),
            endDate = LocalDate(2025, 1, 12)
        ).first()

        // ASSERT
        assertEquals(2, retrievedList.size)
        val retrievedIds = retrievedList.map { it.id }.toSet()
        assertTrue(retrievedIds.contains(entry2.id))
        assertTrue(retrievedIds.contains(entry3.id))
    }

    @Test
    fun getEntriesForDateRange_WHEN_rangeHasNoEntries_THEN_returnsEmptyList() = runTest {
        // ARRANGE
        dao.insert(entry1)

        // ACT
        val retrievedList = dao.getEntriesForDateRange(
            startDate = LocalDate(2025, 2, 1),
            endDate = LocalDate(2025, 2, 5)
        ).first()

        // ASSERT
        assertTrue(retrievedList.isEmpty())
    }
}
