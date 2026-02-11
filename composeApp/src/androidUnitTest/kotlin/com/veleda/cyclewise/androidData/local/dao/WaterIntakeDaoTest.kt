package com.veleda.cyclewise.androidData.local.dao

import com.veleda.cyclewise.KoinTestRule
import com.veleda.cyclewise.androidData.local.database.PeriodDatabase
import com.veleda.cyclewise.androidData.local.entities.WaterIntakeEntity
import com.veleda.cyclewise.testutil.TestData
import com.veleda.cyclewise.testutil.testDatabaseModule
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

@RunWith(RobolectricTestRunner::class)
class WaterIntakeDaoTest : KoinTest {

    @get:Rule
    val koinRule = KoinTestRule(listOf(testDatabaseModule))

    private val dao: WaterIntakeDao by inject()
    private val db: PeriodDatabase by inject()

    @After
    fun tearDown() {
        db.close()
    }

    // --- Test Data ---
    private val intake1 = WaterIntakeEntity(
        date = "2025-01-10",
        cups = 5,
        createdAt = TestData.INSTANT,
        updatedAt = TestData.INSTANT
    )
    private val intake2 = WaterIntakeEntity(
        date = "2025-01-11",
        cups = 3,
        createdAt = TestData.INSTANT,
        updatedAt = TestData.INSTANT
    )

    // --- Tests for upsert() ---

    @Test
    fun upsert_WHEN_newRecord_THEN_insertsSuccessfully() = runTest {
        // ACT
        dao.upsert(intake1)

        // ASSERT
        val retrieved = dao.getForDate("2025-01-10")
        assertNotNull(retrieved)
        assertEquals(5, retrieved.cups)
    }

    @Test
    fun upsert_WHEN_existingDate_THEN_replacesRecord() = runTest {
        // ARRANGE
        dao.upsert(intake1)

        // ACT
        val updated = intake1.copy(cups = 8, updatedAt = TestData.INSTANT)
        dao.upsert(updated)

        // ASSERT
        val retrieved = dao.getForDate("2025-01-10")
        assertNotNull(retrieved)
        assertEquals(8, retrieved.cups)
    }

    // --- Tests for getForDate() ---

    @Test
    fun getForDate_WHEN_exists_THEN_returnsEntity() = runTest {
        // ARRANGE
        dao.upsert(intake1)

        // ACT
        val retrieved = dao.getForDate("2025-01-10")

        // ASSERT
        assertNotNull(retrieved)
        assertEquals("2025-01-10", retrieved.date)
        assertEquals(5, retrieved.cups)
    }

    @Test
    fun getForDate_WHEN_notExists_THEN_returnsNull() = runTest {
        // ACT
        val retrieved = dao.getForDate("2025-01-10")

        // ASSERT
        assertNull(retrieved)
    }

    // --- Tests for getForDates() ---

    @Test
    fun getForDates_WHEN_multipleExist_THEN_returnsBatch() = runTest {
        // ARRANGE
        dao.upsert(intake1)
        dao.upsert(intake2)

        // ACT
        val results = dao.getForDates(listOf("2025-01-10", "2025-01-11"))

        // ASSERT
        assertEquals(2, results.size)
        val dates = results.map { it.date }.toSet()
        assertEquals(setOf("2025-01-10", "2025-01-11"), dates)
    }

    @Test
    fun getForDates_WHEN_someMissing_THEN_returnsOnlyExisting() = runTest {
        // ARRANGE
        dao.upsert(intake1)

        // ACT
        val results = dao.getForDates(listOf("2025-01-10", "2025-01-12"))

        // ASSERT
        assertEquals(1, results.size)
        assertEquals("2025-01-10", results[0].date)
    }

    @Test
    fun getForDates_WHEN_emptyList_THEN_returnsEmpty() = runTest {
        // ACT
        val results = dao.getForDates(emptyList())

        // ASSERT
        assertEquals(0, results.size)
    }
}
