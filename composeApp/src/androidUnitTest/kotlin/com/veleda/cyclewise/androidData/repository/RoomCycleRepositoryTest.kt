package com.veleda.cyclewise.androidData.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.benasher44.uuid.uuid4
import com.veleda.cyclewise.androidData.local.database.PeriodDatabase
import com.veleda.cyclewise.domain.models.*
import com.veleda.cyclewise.domain.repository.PeriodRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.*
import kotlin.time.Clock
import kotlin.time.Instant
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
import org.robolectric.annotation.Config
import kotlin.test.*

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE)
class RoomCycleRepositoryTest : KoinTest {

    // --- SETUP ---
    private val repository: PeriodRepository by inject()
    private val db: PeriodDatabase by inject()

    private val testModule = module {
        // Provide an in-memory database for the test environment
        single {
            Room.inMemoryDatabaseBuilder(
                ApplicationProvider.getApplicationContext(),
                PeriodDatabase::class.java
            )
            .allowMainThreadQueries()
            .build()
        }
        // Provide all DAOs from the database
        single { get<PeriodDatabase>().periodDao() }
        single { get<PeriodDatabase>().dailyEntryDao() }
        single { get<PeriodDatabase>().symptomDao() }
        single { get<PeriodDatabase>().symptomLogDao() }
        single { get<PeriodDatabase>().medicationDao() }
        single { get<PeriodDatabase>().medicationLogDao() }
        single { get<PeriodDatabase>().periodLogDao() }

        // Provide the real repository implementation
        single<PeriodRepository> {
            RoomPeriodRepository(
                db = get(),
                periodDao = get(),
                dailyEntryDao = get(),
                symptomDao = get(),
                symptomLogDao = get(),
                medicationDao = get(),
                medicationLogDao = get(),
                periodLogDao = get()
            )
        }
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

    // --- Test Data Helpers ---
    private val testDate = LocalDate(2025, 8, 15)
    private val testNow = Instant.fromEpochMilliseconds(Clock.System.now().toEpochMilliseconds())

    private fun createBaseDailyEntry(id: String, date: LocalDate) = DailyEntry(
        id = id,
        entryDate = date,
        dayInCycle = 1,
        createdAt = testNow,
        updatedAt = testNow
    )

    private fun createHeavyPeriodLog(entryId: String) = PeriodLog(
        id = uuid4().toString(),
        entryId = entryId,
        flowIntensity = FlowIntensity.HEAVY,
        createdAt = testNow,
        updatedAt = testNow
    )

    // --- Tests for saveFullLog() and getFullLogForDate() ---

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun saveFullLog_and_getFullLogForDate_savesAndRetrievesAllDataCorrectly() = runTest {
        // --- ARRANGE ---
        // 1. Pre-populate libraries and parent period (required for dayInCycle calc but not data storage logic)
        repository.startNewPeriod(testDate.minus(5, DateTimeUnit.DAY))

        val symptom1 = repository.createOrGetSymptomInLibrary("Cramps", SymptomCategory.PAIN)
        val medication1 = repository.createOrGetMedicationInLibrary("Ibuprofen")

        val baseEntry = createBaseDailyEntry("entry-1", testDate)

        // 2. Create the complex log object to save.
        val originalLog = FullDailyLog(
            entry = baseEntry,
            periodLog = createHeavyPeriodLog("entry-1"), // <-- NEW: Include PeriodLog
            symptomLogs = listOf(
                SymptomLog("slog-1", "entry-1", symptom1.id, 4, testNow)
            ),
            medicationLogs = listOf(
                MedicationLog("mlog-1", "entry-1", medication1.id, testNow)
            )
        )

        // --- ACT ---
        repository.saveFullLog(originalLog)
        val retrievedLog = repository.getFullLogForDate(testDate)

        // --- ASSERT ---
        assertNotNull(retrievedLog)
        assertEquals(originalLog.entry.id, retrievedLog.entry.id)
        assertNotNull(retrievedLog.periodLog, "PeriodLog should be retrieved.") // <-- NEW ASSERT
        assertEquals(FlowIntensity.HEAVY, retrievedLog.periodLog!!.flowIntensity) // <-- NEW ASSERTION
        assertEquals(originalLog.symptomLogs.toSet(), retrievedLog.symptomLogs.toSet())
        assertEquals(originalLog.medicationLogs.toSet(), retrievedLog.medicationLogs.toSet())
    }

    @Test
    fun saveFullLog_WHEN_FlowIsCleared_THEN_PeriodLogIsDeleted() = runTest {
        // ARRANGE: Save an initial log with flow data.
        repository.startNewPeriod(testDate.minus(5, DateTimeUnit.DAY))
        val initialLog = FullDailyLog(
            entry = createBaseDailyEntry("entry-1", testDate),
            periodLog = createHeavyPeriodLog("entry-1"),
        )
        repository.saveFullLog(initialLog)
        assertNotNull(repository.getFullLogForDate(testDate)?.periodLog, "Precondition: PeriodLog should exist")

        // ACT: Save the log again, explicitly setting PeriodLog to null (i.e., clearing flow/period data).
        val clearedLog = initialLog.copy(periodLog = null)
        repository.saveFullLog(clearedLog)

        // ASSERT
        val retrievedLog = repository.getFullLogForDate(testDate)
        assertNotNull(retrievedLog)
        assertNull(retrievedLog.periodLog, "PeriodLog should be null after clearing flow data.")
    }

    // --- Tests for Period/Cycle Logic (Unchanged but using new models) ---

    @Test
    fun saveFullLog_WHEN_calledAgainForSameDay_THEN_updatesDataCorrectly() = runTest {
        // ARRANGE: Save an initial log
        repository.startNewPeriod(testDate.minus(5, DateTimeUnit.DAY))
        val symptom1 = repository.createOrGetSymptomInLibrary("Cramps", SymptomCategory.PAIN)
        val symptom2 = repository.createOrGetSymptomInLibrary("Headache", SymptomCategory.PAIN)
        val initialLog = FullDailyLog(
            entry = createBaseDailyEntry("entry-1", testDate),
            periodLog = createHeavyPeriodLog("entry-1"),
            symptomLogs = listOf(SymptomLog("slog-1", "entry-1", symptom1.id, 3, testNow))
        )
        repository.saveFullLog(initialLog)

        // Create an updated version of the log (new symptom, cleared flow data).
        val updatedLog = initialLog.copy(
            periodLog = null,
            symptomLogs = listOf(SymptomLog("slog-2", "entry-1", symptom2.id, 5, testNow))
        )

        // ACT
        repository.saveFullLog(updatedLog)
        val retrievedLog = repository.getFullLogForDate(testDate)

        // ASSERT
        assertNotNull(retrievedLog)
        assertNull(retrievedLog.periodLog, "PeriodLog should be cleared.")
        assertEquals(1, retrievedLog.symptomLogs.size)
        assertEquals(symptom2.id, retrievedLog.symptomLogs.first().symptomId, "The symptom list should have been replaced.")
    }

    // --- Tests for Reactive Flows (getAllPeriods, etc.) ---

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun getAllPeriods_WHEN_databaseChanges_THEN_flowEmitsUpdatedList() = runTest {
        repository.getAllPeriods().test {
            // 1. Initial emission is empty
            assertEquals(0, awaitItem().size, "Initially, list should be empty")

            // 2. Insert a cycle (ASYNCHRONOUS operation)
            val cycle1 = repository.startNewPeriod(LocalDate(2025, 1, 1))

            advanceUntilIdle()
            assertEquals(1, awaitItem().size, "After insert, list should have one cycle")

            // 3. Update the cycle (ASYNCHRONOUS operation)
            repository.updatePeriodEndDate(cycle1.id, LocalDate(2025, 1, 5))

            advanceUntilIdle()
            val updatedList = awaitItem()
            assertEquals(1, updatedList.size)
            assertEquals(LocalDate(2025, 1, 5), updatedList.first().endDate)
        }
    }

    // --- Tests for Library Get-Or-Create Logic ---
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun createOrGetSymptomInLibrary_WHEN_symptomExists_THEN_returnsExistingAndDoesNotChangeCategory() = runTest {
        // --- ARRANGE ---
        val symptom1 = repository.createOrGetSymptomInLibrary("Headache", SymptomCategory.PAIN)

        // --- ACT ---
        val symptom2 = repository.createOrGetSymptomInLibrary("Headache", SymptomCategory.MOOD)

        // --- ASSERT ---
        assertEquals(symptom1.id, symptom2.id, "Should return the existing symptom, not create a new one")

        // This is crucial: allow the background invalidation from Room to complete
        // and the Flow to emit its new value.
        advanceUntilIdle()

        // Use Turbine to safely test the Flow emission.
        repository.getSymptomLibrary().test {
            // awaitItem() suspends until it receives the first list from the Flow.
            val libraryList = awaitItem()

            // Find the specific item we care about to make the test robust.
            val headacheInDb = libraryList.firstOrNull { it.id == symptom1.id }

            // Assert that the item exists and its original category was preserved.
            assertNotNull(headacheInDb)
            assertEquals(SymptomCategory.PAIN, headacheInDb.category, "The category of the original symptom should not have been changed")

            // We are done with this flow, cancel the collector and ignore any other events.
            cancelAndIgnoreRemainingEvents()
        }
    }

    // --- Tests for Validation Logic ---

    @Test
    fun isDateRangeAvailable_WHEN_overlapExists_THEN_returnsFalse() = runTest {
        // ARRANGE
        repository.createCompletedPeriod(LocalDate(2025, 5, 10), LocalDate(2025, 5, 15))

        // ACT
        val isAvailable = repository.isDateRangeAvailable(
            startDate = LocalDate(2025, 5, 12),
            endDate = LocalDate(2025, 5, 17)
        )

        // ASSERT
        assertFalse(isAvailable)
    }

    @Test
    fun getAllSymptomLogs_WHEN_logsExist_THEN_returnsAllLogsFromAllCycles() = runTest {
        // ARRANGE
        // Create two separate periods and entries
        val cycle1 = repository.startNewPeriod(LocalDate(2025, 1, 1))
        val cycle2 = repository.createCompletedPeriod(LocalDate(2025, 2, 1), LocalDate(2025, 2, 28))
        val symptom1 = repository.createOrGetSymptomInLibrary("Headache", SymptomCategory.PAIN)

        val log1 = FullDailyLog(DailyEntry("entry-1", LocalDate(2025, 1, 5), 5, createdAt = Clock.System.now(), updatedAt = Clock.System.now()), symptomLogs = listOf(SymptomLog("slog-1", "entry-1", symptom1.id, 3, Clock.System.now())))
        val log2 = FullDailyLog(DailyEntry("entry-2", LocalDate(2025, 2, 5), 5, createdAt = Clock.System.now(), updatedAt = Clock.System.now()), symptomLogs = listOf(SymptomLog("slog-2", "entry-2", symptom1.id, 4, Clock.System.now())))
        val log3WithNoSymptoms = FullDailyLog(DailyEntry("entry-3", LocalDate(2025, 2, 6), 6, createdAt = Clock.System.now(), updatedAt = Clock.System.now()))

        repository.saveFullLog(log1)
        repository.saveFullLog(log2)
        repository.saveFullLog(log3WithNoSymptoms)

        // ACT
        val allLogs = repository.getAllSymptomLogs()

        // ASSERT
        assertEquals(2, allLogs.size, "Should fetch all symptom logs regardless of cycle")
        assertTrue(allLogs.any { it.id == "slog-1" })
        assertTrue(allLogs.any { it.id == "slog-2" })
    }

    @Test
    fun getAllMedicationLogs_WHEN_logsExist_THEN_returnsAllLogs() = runTest {
        // ARRANGE
        val cycle1 = repository.startNewPeriod(LocalDate(2025, 1, 1))
        val med1 = repository.createOrGetMedicationInLibrary("Ibuprofen")

        val log1 = FullDailyLog(DailyEntry("entry-1", LocalDate(2025, 1, 5), 5, createdAt = Clock.System.now(), updatedAt = Clock.System.now()), medicationLogs = listOf(MedicationLog("mlog-1", "entry-1", med1.id, Clock.System.now())))
        repository.saveFullLog(log1)

        // ACT
        val allLogs = repository.getAllMedicationLogs()

        // ASSERT
        assertEquals(1, allLogs.size)
        assertEquals("mlog-1", allLogs.first().id)
    }
}