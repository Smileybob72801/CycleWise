package com.veleda.cyclewise.androidData.local.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.veleda.cyclewise.androidData.local.database.PeriodDatabase
import com.veleda.cyclewise.androidData.local.entities.DailyEntryEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.time.Clock
import kotlinx.datetime.LocalDate
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
import kotlin.test.*

@RunWith(RobolectricTestRunner::class)
class DailyEntryDaoTest : KoinTest {

    // --- SETUP ---
    private val dao: DailyEntryDao by inject()
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
    // The parent cycle is no longer needed as the foreign key has been removed.
    private val entry1 = DailyEntryEntity(
        id = "entry-uuid-1",
        entryDate = LocalDate(2025, 1, 10),
        dayInCycle = 10,
        customTags = "[]",
        createdAt = Clock.System.now(), updatedAt = Clock.System.now()
    )
    private val entry2 = DailyEntryEntity(
        id = "entry-uuid-2",
        entryDate = LocalDate(2025, 1, 12),
        dayInCycle = 12,
        moodScore = 4,
        customTags = "[]",
        createdAt = Clock.System.now(), updatedAt = Clock.System.now()
    )
    private val entry3 = DailyEntryEntity(
        id = "entry-uuid-3",
        entryDate = LocalDate(2025, 1, 11), // Note the non-chronological date
        dayInCycle = 11,
        customTags = "[]",
        createdAt = Clock.System.now(), updatedAt = Clock.System.now()
    )

    // --- Tests for insert() and update() ---

    @Test
    fun insert_WHEN_entryIsNew_THEN_addsToDatabase() = runTest {
        // ARRANGE - No arrangement needed
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
        dao.insert(entry1) // Initial moodScore is null

        // Create an entry with the same ID but a different moodScore
        val updatedEntry = entry1.copy(moodScore = 5)

        // ACT
        // OnConflictStrategy.REPLACE should overwrite the original entry
        dao.insert(updatedEntry)

        // ASSERT
        val retrieved = dao.getEntryForDate(entry1.entryDate).first()
        assertNotNull(retrieved)
        // FIX: Verify the total count of entries is still 1, confirming a replace, not an add.
        assertEquals(1, dao.getAllEntries().first().size)
        assertEquals(5, retrieved.moodScore)
    }

    @Test
    fun update_WHEN_entryExists_THEN_modifiesTheRecord() = runTest {
        // ARRANGE
        dao.insert(entry2) // Initial moodScore is 4

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
        dao.insert(entry1)

        val retrieved = dao.getEntryForDate(LocalDate(2025, 1, 10)).first()

        assertNotNull(retrieved)
        assertEquals(entry1.id, retrieved.id)
    }

    @Test
    fun getEntryForDate_WHEN_entryDoesNotExist_THEN_returnsNull() = runTest {
        val retrieved = dao.getEntryForDate(LocalDate(2025, 1, 10)).first()
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
        dao.insert(entry1)

        val retrievedList = dao.getEntriesForDateRange(
            startDate = LocalDate(2025, 2, 1),
            endDate = LocalDate(2025, 2, 5)
        ).first()

        assertTrue(retrievedList.isEmpty())
    }
}