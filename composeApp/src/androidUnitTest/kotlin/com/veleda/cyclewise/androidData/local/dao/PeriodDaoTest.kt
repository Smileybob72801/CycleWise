package com.veleda.cyclewise.androidData.local.dao

import com.veleda.cyclewise.KoinTestRule
import com.veleda.cyclewise.androidData.local.database.PeriodDatabase
import com.veleda.cyclewise.androidData.local.entities.PeriodEntity
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
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class PeriodDaoTest : KoinTest {

    @get:Rule
    val koinRule = KoinTestRule(listOf(testDatabaseModule))

    private val dao: PeriodDao by inject()
    private val db: PeriodDatabase by inject()

    @After
    fun tearDown() {
        db.close()
    }

    // --- Test Data ---
    private val cyclePast = PeriodEntity(
        uuid = "uuid-past",
        startDate = LocalDate(2025, 1, 1),
        endDate = LocalDate(2025, 1, 5),
        createdAt = TestData.INSTANT, updatedAt = TestData.INSTANT
    )
    private val cyclePresent = PeriodEntity(
        uuid = "uuid-present",
        startDate = LocalDate(2025, 2, 1),
        endDate = LocalDate(2025, 2, 5),
        createdAt = TestData.INSTANT, updatedAt = TestData.INSTANT
    )
    private val cycleOngoing = PeriodEntity(
        uuid = "uuid-ongoing",
        startDate = LocalDate(2025, 3, 1),
        endDate = null,
        createdAt = TestData.INSTANT, updatedAt = TestData.INSTANT
    )

    // --- Tests for getAllPeriods() ---

    @Test
    fun getAllCycles_WHEN_databaseIsEmpty_THEN_returnsEmptyFlow() = runTest {
        // ACT
        val cycles = dao.getAllPeriods().first()

        // ASSERT
        assertTrue(cycles.isEmpty(), "Expected an empty list when DB is empty")
    }

    @Test
    fun getAllCycles_WHEN_dataExists_THEN_returnsAllCyclesSortedByStartDateDesc() = runTest {
        // ARRANGE
        dao.insert(cyclePast)
        dao.insert(cycleOngoing)
        dao.insert(cyclePresent)

        // ACT
        val cycles = dao.getAllPeriods().first()

        // ASSERT
        assertEquals(3, cycles.size)
        assertEquals("uuid-ongoing", cycles[0].uuid)
        assertEquals("uuid-present", cycles[1].uuid)
        assertEquals("uuid-past", cycles[2].uuid)
    }

    // --- Tests for getById() and getByUuid() ---

    @Test
    fun getById_WHEN_idExists_THEN_returnsCorrectEntity() = runTest {
        // ARRANGE
        dao.insert(cyclePast)

        // ACT
        val retrieved = dao.getById(1)

        // ASSERT
        assertNotNull(retrieved)
        assertEquals("uuid-past", retrieved.uuid)
    }

    @Test
    fun getByUuid_WHEN_uuidDoesNotExist_THEN_returnsNull() = runTest {
        // ACT
        val retrieved = dao.getByUuid("non-existent-uuid")

        // ASSERT
        assertNull(retrieved, "Expected null for a non-existent UUID")
    }

    // --- Tests for insert() and update() ---

    @Test
    fun update_WHEN_entityExists_THEN_modifiesTheRecord() = runTest {
        // ARRANGE
        val initialCycle = PeriodEntity(
            uuid = "uuid-past",
            startDate = LocalDate(2025, 1, 1),
            endDate = LocalDate(2025, 1, 5),
            createdAt = TestData.INSTANT, updatedAt = TestData.INSTANT
        )
        dao.insert(initialCycle)
        val insertedCycle = dao.getByUuid("uuid-past")
        assertNotNull(insertedCycle, "Precondition failed: Period was not inserted correctly.")

        // ACT
        val modifiedCycle = insertedCycle.copy(endDate = LocalDate(2025, 1, 10))
        dao.update(modifiedCycle)

        // ASSERT
        val retrievedAfterUpdate = dao.getByUuid("uuid-past")
        assertNotNull(retrievedAfterUpdate)
        assertEquals(LocalDate(2025, 1, 10), retrievedAfterUpdate.endDate)
    }

    // --- Tests for getOngoingPeriod() ---

    @Test
    fun getOngoingCycle_WHEN_oneOngoingCycleExists_THEN_returnsIt() = runTest {
        // ARRANGE
        dao.insert(cyclePast)
        dao.insert(cycleOngoing)

        // ACT
        val retrieved = dao.getOngoingPeriod().first()

        // ASSERT
        assertNotNull(retrieved)
        assertEquals("uuid-ongoing", retrieved.uuid)
    }

    @Test
    fun getOngoingCycle_WHEN_noOngoingCycleExists_THEN_returnsNull() = runTest {
        // ARRANGE
        dao.insert(cyclePast)
        dao.insert(cyclePresent)

        // ACT
        val retrieved = dao.getOngoingPeriod().first()

        // ASSERT
        assertNull(retrieved, "Expected null when no cycle has a null end_date")
    }

    // --- Tests for getOverlappingPeriodsCount() ---

    @Test
    fun getOverlappingPeriodsCount_WHEN_rangeExactlyMatches_THEN_returnsOne() = runTest {
        // ARRANGE
        dao.insert(cyclePast)

        // ACT
        val count = dao.getOverlappingPeriodsCount(cyclePast.startDate, cyclePast.endDate!!)

        // ASSERT
        assertEquals(1, count)
    }

    @Test
    fun getOverlappingPeriodsCount_WHEN_rangeIsContainedWithin_THEN_returnsOne() = runTest {
        // ARRANGE
        dao.insert(cyclePast)

        // ACT
        val count = dao.getOverlappingPeriodsCount(
            startDate = LocalDate(2025, 1, 2),
            endDate = LocalDate(2025, 1, 4)
        )

        // ASSERT
        assertEquals(1, count)
    }

    @Test
    fun getOverlappingPeriodsCount_WHEN_rangeContains_THEN_returnsOne() = runTest {
        // ARRANGE
        dao.insert(cyclePast)

        // ACT
        val count = dao.getOverlappingPeriodsCount(
            startDate = LocalDate(2024, 12, 30),
            endDate = LocalDate(2025, 1, 10)
        )

        // ASSERT
        assertEquals(1, count)
    }

    @Test
    fun getOverlappingPeriodsCount_WHEN_rangeOverlapsOngoingCycle_THEN_returnsOne() = runTest {
        // ARRANGE
        dao.insert(cycleOngoing)

        // ACT
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
        dao.insert(cyclePast)

        // ACT
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
        dao.insert(cyclePast)

        // ACT
        val count = dao.getOverlappingPeriodsCount(
            startDate = LocalDate(2025, 1, 6),
            endDate = LocalDate(2025, 1, 10)
        )

        // ASSERT
        assertEquals(0, count)
    }
}
