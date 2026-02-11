package com.veleda.cyclewise.androidData.local.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.veleda.cyclewise.androidData.local.database.PeriodDatabase
import com.veleda.cyclewise.androidData.local.entities.*
import com.veleda.cyclewise.domain.models.SymptomCategory
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
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class SymptomLogDaoTest : KoinTest {

    // --- SETUP ---
    private val dao: SymptomLogDao by inject()
    private val db: PeriodDatabase by inject()

    // We need parent DAOs to set up foreign key relationships
    private val dailyEntryDao: DailyEntryDao by inject()
    private val symptomDao: SymptomDao by inject()

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
        single { get<PeriodDatabase>().periodLogDao() }
        single { get<PeriodDatabase>().waterIntakeDao() }
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
    private val parentEntry1 = DailyEntryEntity("entry-1", LocalDate(2025, 1, 5), 5, customTags = "[]", note = null, createdAt = Clock.System.now(), updatedAt = Clock.System.now())
    private val parentEntry2 = DailyEntryEntity("entry-2", LocalDate(2025, 1, 6), 6, customTags = "[]", note = null, createdAt = Clock.System.now(), updatedAt = Clock.System.now())
    private val parentSymptom1 = SymptomEntity("symptom-1", "Cramps", SymptomCategory.PAIN, Clock.System.now())
    private val parentSymptom2 = SymptomEntity("symptom-2", "Anxiety", SymptomCategory.MOOD, Clock.System.now())

    private val log1_symptom1_entry1 = SymptomLogEntity("log-1", "entry-1", "symptom-1", 4, Clock.System.now())
    private val log2_symptom2_entry1 = SymptomLogEntity("log-2", "entry-1", "symptom-2", 2, Clock.System.now())
    private val log3_symptom1_entry2 = SymptomLogEntity("log-3", "entry-2", "symptom-1", 5, Clock.System.now())

    // --- Tests for insertAll() ---

    @Test
    fun insertAll_WHEN_logsAreNew_THEN_addsAllToDatabase() = runTest {
        // ARRANGE: Insert all required parent data
        dailyEntryDao.insert(parentEntry1)
        symptomDao.insert(parentSymptom1)
        symptomDao.insert(parentSymptom2)

        // ACT
        dao.insertAll(listOf(log1_symptom1_entry1, log2_symptom2_entry1))

        // ASSERT
        val retrieved = dao.getLogsForEntry("entry-1").first()
        assertEquals(2, retrieved.size)
    }

    @Test
    fun insertAll_WHEN_logWithSameIdExists_THEN_onConflictReplaceUpdatesTheRecord() = runTest {
        // ARRANGE
        dailyEntryDao.insert(parentEntry1)
        symptomDao.insert(parentSymptom1)
        dao.insertAll(listOf(log1_symptom1_entry1)) // Initial severity is 4

        // Create a new log with the same ID but a different severity
        val updatedLog = log1_symptom1_entry1.copy(severity = 1)

        // ACT
        dao.insertAll(listOf(updatedLog))

        // ASSERT
        val retrieved = dao.getLogsForEntry("entry-1").first()
        assertEquals(1, retrieved.size, "Should still only be one log entry")
        assertEquals(1, retrieved.first().severity, "The severity should have been updated")
    }

    // --- Tests for getLogsForEntry() and getLogsForEntries() ---

    @Test
    fun getLogsForEntry_WHEN_dataExists_THEN_returnsCorrectLogs() = runTest {
        // ARRANGE
        dailyEntryDao.insert(parentEntry1)
        dailyEntryDao.insert(parentEntry2)
        symptomDao.insert(parentSymptom1)
        symptomDao.insert(parentSymptom2)
        dao.insertAll(listOf(log1_symptom1_entry1, log2_symptom2_entry1, log3_symptom1_entry2))

        // ACT
        val logsForEntry1 = dao.getLogsForEntry("entry-1").first()
        val logsForEntry2 = dao.getLogsForEntry("entry-2").first()

        // ASSERT
        assertEquals(2, logsForEntry1.size)
        assertTrue(logsForEntry1.any { it.id == "log-1" })
        assertTrue(logsForEntry1.any { it.id == "log-2" })

        assertEquals(1, logsForEntry2.size)
        assertTrue(logsForEntry2.any { it.id == "log-3" })
    }

    @Test
    fun getLogsForEntries_WHEN_multipleIdsProvided_THEN_returnsAllAssociatedLogs() = runTest {
        // ARRANGE
        dailyEntryDao.insert(parentEntry1)
        dailyEntryDao.insert(parentEntry2)
        symptomDao.insert(parentSymptom1)
        symptomDao.insert(parentSymptom2)
        dao.insertAll(listOf(log1_symptom1_entry1, log2_symptom2_entry1, log3_symptom1_entry2))

        // ACT
        val allLogs = dao.getLogsForEntries(listOf("entry-1", "entry-2")).first()

        // ASSERT
        assertEquals(3, allLogs.size)
    }

    // --- Tests for deleteLogsForEntry() ---

    @Test
    fun deleteLogsForEntry_WHEN_called_THEN_removesOnlyLogsForThatEntry() = runTest {
        // ARRANGE
        dailyEntryDao.insert(parentEntry1)
        dailyEntryDao.insert(parentEntry2)
        symptomDao.insert(parentSymptom1)
        dao.insertAll(listOf(log1_symptom1_entry1, log3_symptom1_entry2))

        var logsForEntry1 = dao.getLogsForEntry("entry-1").first()
        assertEquals(1, logsForEntry1.size, "Precondition failed: Entry 1 should have 1 log")

        // ACT
        dao.deleteLogsForEntry("entry-1")

        // ASSERT
        logsForEntry1 = dao.getLogsForEntry("entry-1").first()
        val logsForEntry2 = dao.getLogsForEntry("entry-2").first()

        assertTrue(logsForEntry1.isEmpty(), "Logs for entry 1 should be deleted")
        assertEquals(1, logsForEntry2.size, "Logs for entry 2 should not be affected")
    }
}