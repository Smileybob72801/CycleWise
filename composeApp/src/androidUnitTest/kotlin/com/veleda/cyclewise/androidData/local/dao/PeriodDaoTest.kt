package com.veleda.cyclewise.androidData.local.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.veleda.cyclewise.androidData.local.database.PeriodDatabase
import com.veleda.cyclewise.androidData.local.entities.PeriodEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
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
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock

@RunWith(RobolectricTestRunner::class)
class PeriodDaoTest : KoinTest {
    private val dao: PeriodDao by inject()

    // We inject the database itself so we can close it in tearDown.
    private val db: PeriodDatabase by inject()

    // Create a temporary Koin module specifically for this test class.
    private val testModule = module {
        // Define how to create the in-memory database.
        single {
            Room.inMemoryDatabaseBuilder(
                ApplicationProvider.getApplicationContext(),
                PeriodDatabase::class.java
            )
                .allowMainThreadQueries()
                .build()
        }
        // Define how to create all the DAOs from the database instance.
        single { get<PeriodDatabase>().periodDao() }
        single { get<PeriodDatabase>().dailyEntryDao() }
        single { get<PeriodDatabase>().symptomDao() }
        single { get<PeriodDatabase>().symptomLogDao() }
        single { get<PeriodDatabase>().medicationDao() }
        single { get<PeriodDatabase>().medicationLogDao() }
        // (Add any other DAOs or test dependencies here)
    }

    // --- Test Data ---
    // Define reusable test entities to keep tests clean and consistent.
    private val cyclePast = PeriodEntity(
        uuid = "uuid-past",
        startDate = LocalDate(2025, 1, 1),
        endDate = LocalDate(2025, 1, 5),
        createdAt = Clock.System.now(), updatedAt = Clock.System.now()
    )
    private val cyclePresent = PeriodEntity(
        uuid = "uuid-present",
        startDate = LocalDate(2025, 2, 1),
        endDate = LocalDate(2025, 2, 5),
        createdAt = Clock.System.now(), updatedAt = Clock.System.now()
    )
    private val cycleOngoing = PeriodEntity(
        uuid = "uuid-ongoing",
        startDate = LocalDate(2025, 3, 1),
        endDate = null, // The key property for an ongoing cycle
        createdAt = Clock.System.now(), updatedAt = Clock.System.now()
    )

    @Before
    fun setUp() {
        // Start a fresh Koin context for each test, using only our test module.
        // This completely isolates the test from the production appModule.
        startKoin {
            modules(testModule)
        }
    }

    @After
    fun tearDown() {
        // Close the database to clear all data.
        db.close()
        // Stop the Koin context to prevent state leakage.
        stopKoin()
    }

    // --- Tests for getAllPeriods() ---

    @Test
    fun getAllCycles_WHEN_databaseIsEmpty_THEN_returnsEmptyFlow() = runTest {
        val cycles = dao.getAllPeriods().first()
        assertTrue(cycles.isEmpty(), "Expected an empty list when DB is empty")
    }

    @Test
    fun getAllCycles_WHEN_dataExists_THEN_returnsAllCyclesSortedByStartDateDesc() = runTest {
        // ARRANGE: Insert in a non-sorted order
        dao.insert(cyclePast)    // Jan 1st
        dao.insert(cycleOngoing) // Mar 1st
        dao.insert(cyclePresent) // Feb 1st

        // ACT
        val cycles = dao.getAllPeriods().first()

        // ASSERT
        assertEquals(3, cycles.size)
        // Verify the DESC order from the SQL query
        assertEquals("uuid-ongoing", cycles[0].uuid)
        assertEquals("uuid-present", cycles[1].uuid)
        assertEquals("uuid-past", cycles[2].uuid)
    }

    // --- Tests for getById() and getByUuid() ---

    @Test
    fun getById_WHEN_idExists_THEN_returnsCorrectEntity() = runTest {
        dao.insert(cyclePast) // Room will assign id=1
        val retrieved = dao.getById(1)
        assertNotNull(retrieved)
        assertEquals("uuid-past", retrieved.uuid)
    }

    @Test
    fun getByUuid_WHEN_uuidDoesNotExist_THEN_returnsNull() = runTest {
        val retrieved = dao.getByUuid("non-existent-uuid")
        assertNull(retrieved, "Expected null for a non-existent UUID")
    }

    // --- Tests for insert() and update() ---

    @Test
    fun update_WHEN_entityExists_THEN_modifiesTheRecord() = runTest {
        // --- ARRANGE ---
        // 1. Create the initial object. Let the `id` use its default value (0).
        val initialCycle = PeriodEntity(
            uuid = "uuid-past",
            startDate = LocalDate(2025, 1, 1),
            endDate = LocalDate(2025, 1, 5),
            createdAt = Clock.System.now(), updatedAt = Clock.System.now()
        )
        dao.insert(initialCycle)

        // 2. Retrieve the object we just inserted to get the auto-generated primary key.
        val insertedCycle = dao.getByUuid("uuid-past")
        assertNotNull(insertedCycle, "Precondition failed: Period was not inserted correctly.")
        // `insertedCycle` now has the correct, database-generated `id`.

        // --- ACT ---
        // 3. Create the modified version based on the *retrieved* object.
        //    This ensures we have the correct primary key for the update operation.
        val modifiedCycle = insertedCycle.copy(endDate = LocalDate(2025, 1, 10))
        dao.update(modifiedCycle)

        // --- ASSERT ---
        // 4. Retrieve the final state and assert the change was successful.
        val retrievedAfterUpdate = dao.getByUuid("uuid-past")
        assertNotNull(retrievedAfterUpdate)
        assertEquals(LocalDate(2025, 1, 10), retrievedAfterUpdate.endDate)
    }

    // --- Tests for getOngoingPeriod() ---

    @Test
    fun getOngoingCycle_WHEN_oneOngoingCycleExists_THEN_returnsIt() = runTest {
        dao.insert(cyclePast)
        dao.insert(cycleOngoing)

        val retrieved = dao.getOngoingPeriod().first()

        assertNotNull(retrieved)
        assertEquals("uuid-ongoing", retrieved.uuid)
    }

    @Test
    fun getOngoingCycle_WHEN_noOngoingCycleExists_THEN_returnsNull() = runTest {
        dao.insert(cyclePast)
        dao.insert(cyclePresent)

        val retrieved = dao.getOngoingPeriod().first()

        assertNull(retrieved, "Expected null when no cycle has a null end_date")
    }

    // --- Tests for getOverlappingPeriodsCount() ---

    @Test
    fun getOverlappingPeriodsCount_WHEN_rangeExactlyMatches_THEN_returnsOne() = runTest {
        dao.insert(cyclePast)
        val count = dao.getOverlappingPeriodsCount(cyclePast.startDate, cyclePast.endDate!!)
        assertEquals(1, count)
    }

    @Test
    fun getOverlappingPeriodsCount_WHEN_rangeIsContainedWithin_THEN_returnsOne() = runTest {
        dao.insert(cyclePast)
        val count = dao.getOverlappingPeriodsCount(
            startDate = LocalDate(2025, 1, 2),
            endDate = LocalDate(2025, 1, 4)
        )
        assertEquals(1, count)
    }

    @Test
    fun getOverlappingPeriodsCount_WHEN_rangeContains_THEN_returnsOne() = runTest {
        dao.insert(cyclePast)
        val count = dao.getOverlappingPeriodsCount(
            startDate = LocalDate(2024, 12, 30),
            endDate = LocalDate(2025, 1, 10)
        )
        assertEquals(1, count)
    }

    @Test
    fun getOverlappingPeriodsCount_WHEN_rangeOverlapsOngoingCycle_THEN_returnsOne() = runTest {
        // ARRANGE: An ongoing cycle starts on March 1st
        dao.insert(cycleOngoing)

        // ACT: Check for a range that starts after the ongoing cycle begins
        val count = dao.getOverlappingPeriodsCount(
            startDate = LocalDate(2025, 3, 15),
            endDate = LocalDate(2025, 3, 20)
        )

        // ASSERT
        assertEquals(1, count, "An ongoing cycle should be considered as overlapping with any future date range")
    }

    @Test
    fun getOverlappingPeriodsCount_WHEN_rangeIsAdjacentAndTouching_THEN_returnsOne() = runTest {
        // ARRANGE
        dao.insert(cyclePast) // Ends on Jan 5th

        // ACT: A new cycle starts on the same day the old one ended
        val count = dao.getOverlappingPeriodsCount(
            startDate = LocalDate(2025, 1, 5),
            endDate = LocalDate(2025, 1, 9)
        )

        // ASSERT
        assertEquals(1, count, "Ranges touching at the boundary should be counted as overlapping")
    }

    @Test
    fun getOverlappingPeriodsCount_WHEN_rangeIsAdjacentButNotTouching_THEN_returnsZero() = runTest {
        // ARRANGE
        dao.insert(cyclePast) // Ends on Jan 5th

        // ACT: A new cycle starts the day after the old one ended
        val count = dao.getOverlappingPeriodsCount(
            startDate = LocalDate(2025, 1, 6),
            endDate = LocalDate(2025, 1, 10)
        )

        // ASSERT
        assertEquals(0, count)
    }
}