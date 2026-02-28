package com.veleda.cyclewise.androidData.local.dao

import com.veleda.cyclewise.KoinTestRule
import com.veleda.cyclewise.androidData.local.database.PeriodDatabase
import com.veleda.cyclewise.domain.models.FlowIntensity
import com.veleda.cyclewise.domain.models.PeriodColor
import com.veleda.cyclewise.domain.models.PeriodConsistency
import com.veleda.cyclewise.testutil.TestData
import com.veleda.cyclewise.testutil.buildDailyEntryEntity
import com.veleda.cyclewise.testutil.buildPeriodLogEntity
import com.veleda.cyclewise.testutil.testDatabaseModule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
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
class PeriodLogDaoTest : KoinTest {

    @get:Rule
    val koinRule = KoinTestRule(listOf(testDatabaseModule))

    private val periodLogDao: PeriodLogDao by inject()
    private val dailyEntryDao: DailyEntryDao by inject()
    private val db: PeriodDatabase by inject()

    private val parentEntry = buildDailyEntryEntity(id = "entry-1")
    private val parentEntry2 = buildDailyEntryEntity(id = "entry-2")

    @Before
    fun setUp() = runTest {
        // Insert parent daily entries for FK constraint
        dailyEntryDao.insert(parentEntry)
        dailyEntryDao.insert(parentEntry2)
    }

    @After
    fun tearDown() {
        db.close()
    }

    // --- Tests for insert() and getLogForEntry() ---

    @Test
    fun insert_WHEN_logInserted_THEN_getLogForEntryReturnsIt() = runTest {
        // ARRANGE
        val log = buildPeriodLogEntity(
            id = "plog-1",
            entryId = "entry-1",
            flowIntensity = FlowIntensity.HEAVY
        )

        // ACT
        periodLogDao.insert(log)
        val retrieved = periodLogDao.getLogForEntry("entry-1").first()

        // ASSERT
        assertNotNull(retrieved)
        assertEquals("plog-1", retrieved.id)
        assertEquals("entry-1", retrieved.entryId)
        assertEquals(FlowIntensity.HEAVY, retrieved.flowIntensity)
    }

    @Test
    fun insert_WHEN_logWithColorAndConsistency_THEN_storesOptionalFields() = runTest {
        // ARRANGE
        val log = buildPeriodLogEntity(
            id = "plog-1",
            entryId = "entry-1",
            flowIntensity = FlowIntensity.MEDIUM,
            periodColor = PeriodColor.DARK_RED,
            periodConsistency = PeriodConsistency.CLOTS_SMALL
        )

        // ACT
        periodLogDao.insert(log)
        val retrieved = periodLogDao.getLogForEntry("entry-1").first()

        // ASSERT
        assertNotNull(retrieved)
        assertEquals(PeriodColor.DARK_RED, retrieved.periodColor)
        assertEquals(PeriodConsistency.CLOTS_SMALL, retrieved.periodConsistency)
    }

    @Test
    fun getLogForEntry_WHEN_noLogExists_THEN_returnsNull() = runTest {
        // ACT
        val retrieved = periodLogDao.getLogForEntry("non-existent").first()

        // ASSERT
        assertNull(retrieved)
    }

    // --- Tests for update() ---

    @Test
    fun update_WHEN_logExists_THEN_modifiesTheRecord() = runTest {
        // ARRANGE
        val log = buildPeriodLogEntity(
            id = "plog-1",
            entryId = "entry-1",
            flowIntensity = FlowIntensity.LIGHT
        )
        periodLogDao.insert(log)

        // ACT
        val updated = log.copy(flowIntensity = FlowIntensity.HEAVY)
        periodLogDao.update(updated)
        val retrieved = periodLogDao.getLogForEntry("entry-1").first()

        // ASSERT
        assertNotNull(retrieved)
        assertEquals(FlowIntensity.HEAVY, retrieved.flowIntensity)
    }

    // --- Tests for deleteLogForEntry() ---

    @Test
    fun deleteLogForEntry_WHEN_logExists_THEN_removesIt() = runTest {
        // ARRANGE
        val log = buildPeriodLogEntity(id = "plog-1", entryId = "entry-1")
        periodLogDao.insert(log)

        // ACT
        periodLogDao.deleteLogForEntry("entry-1")
        val retrieved = periodLogDao.getLogForEntry("entry-1").first()

        // ASSERT
        assertNull(retrieved)
    }

    @Test
    fun deleteLogForEntry_WHEN_noLogExists_THEN_noOpWithoutError() = runTest {
        // ACT & ASSERT — should not throw
        periodLogDao.deleteLogForEntry("non-existent")
    }

    // --- Tests for getAllPeriodLogs() ---

    @Test
    fun getAllPeriodLogs_WHEN_databaseIsEmpty_THEN_returnsEmptyList() = runTest {
        // ACT
        val logs = periodLogDao.getAllPeriodLogs().first()

        // ASSERT
        assertTrue(logs.isEmpty())
    }

    @Test
    fun getAllPeriodLogs_WHEN_multipleLogsExist_THEN_returnsAllLogs() = runTest {
        // ARRANGE
        periodLogDao.insert(buildPeriodLogEntity(id = "plog-1", entryId = "entry-1", flowIntensity = FlowIntensity.LIGHT))
        periodLogDao.insert(buildPeriodLogEntity(id = "plog-2", entryId = "entry-2", flowIntensity = FlowIntensity.HEAVY))

        // ACT
        val logs = periodLogDao.getAllPeriodLogs().first()

        // ASSERT
        assertEquals(2, logs.size)
    }

    // --- Tests for deleteAll() ---

    @Test
    fun deleteAll_WHEN_logsExist_THEN_removesAllLogs() = runTest {
        // ARRANGE
        periodLogDao.insert(buildPeriodLogEntity(id = "plog-1", entryId = "entry-1"))
        periodLogDao.insert(buildPeriodLogEntity(id = "plog-2", entryId = "entry-2"))

        // ACT
        periodLogDao.deleteAll()
        val logs = periodLogDao.getAllPeriodLogs().first()

        // ASSERT
        assertTrue(logs.isEmpty())
    }

    // --- Tests for REPLACE conflict strategy ---

    @Test
    fun insert_WHEN_duplicateIdInserted_THEN_replacesExistingRecord() = runTest {
        // ARRANGE
        val original = buildPeriodLogEntity(
            id = "plog-1",
            entryId = "entry-1",
            flowIntensity = FlowIntensity.LIGHT
        )
        periodLogDao.insert(original)

        // ACT — insert again with same id but different flow
        val replacement = original.copy(flowIntensity = FlowIntensity.HEAVY)
        periodLogDao.insert(replacement)
        val retrieved = periodLogDao.getLogForEntry("entry-1").first()

        // ASSERT
        assertNotNull(retrieved)
        assertEquals(FlowIntensity.HEAVY, retrieved.flowIntensity)
    }
}
