package com.veleda.cyclewise.androidData.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.veleda.cyclewise.androidData.local.database.CycleDatabase
import com.veleda.cyclewise.domain.models.*
import com.veleda.cyclewise.domain.repository.CycleRepository
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
    private val repository: CycleRepository by inject()
    private val db: CycleDatabase by inject()

    private val testModule = module {
        // Provide an in-memory database for the test environment
        single {
            Room.inMemoryDatabaseBuilder(
                ApplicationProvider.getApplicationContext(),
                CycleDatabase::class.java
            )
            .allowMainThreadQueries()
            .build()
        }
        // Provide all DAOs from the database
        single { get<CycleDatabase>().cycleDao() }
        single { get<CycleDatabase>().dailyEntryDao() }
        single { get<CycleDatabase>().symptomDao() }
        single { get<CycleDatabase>().symptomLogDao() }
        single { get<CycleDatabase>().medicationDao() }
        single { get<CycleDatabase>().medicationLogDao() }

        // Provide the real repository implementation
        single<CycleRepository> {
            RoomCycleRepository(
                db = get(),
                cycleDao = get(),
                dailyEntryDao = get(),
                symptomDao = get(),
                symptomLogDao = get(),
                medicationDao = get(),
                medicationLogDao = get()
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

    // --- Tests for saveFullLog() and getFullLogForDate() ---

    @Test
    fun saveFullLog_and_getFullLogForDate_savesAndRetrievesAllDataCorrectly() = runTest {
        // --- ARRANGE ---
        val testDate = LocalDate(2025, 8, 15)

        // Get the current time and explicitly truncate it to millisecond precision.
        // This ensures that the object we create in memory has the exact same precision
        // as the object that will be retrieved from the database (because the converter truncates).
        val now = Clock.System.now()
        val nowMillis = Instant.fromEpochMilliseconds(now.toEpochMilliseconds())

        // 1. Pre-populate libraries and parent cycle
        val cycle = repository.startNewCycle(testDate.minus(5, DateTimeUnit.DAY))
        val symptom1 = repository.createOrGetSymptomInLibrary("Cramps", SymptomCategory.PAIN)
        val medication1 = repository.createOrGetMedicationInLibrary("Ibuprofen")

        // 2. Create the complex log object to save, using the truncated timestamp
        val originalLog = FullDailyLog(
            entry = DailyEntry(
                id = "entry-1", cycleId = cycle.id, entryDate = testDate, dayInCycle = 6,
                createdAt = nowMillis, updatedAt = nowMillis
            ),
            symptomLogs = listOf(
                SymptomLog("slog-1", "entry-1", symptom1.id, 4, nowMillis)
            ),
            medicationLogs = listOf(
                MedicationLog("mlog-1", "entry-1", medication1.id, nowMillis)
            )
        )

        // --- ACT ---
        repository.saveFullLog(originalLog)
        val retrievedLog = repository.getFullLogForDate(testDate)

        // --- ASSERT ---
        assertNotNull(retrievedLog)
        assertEquals(originalLog.entry.id, retrievedLog.entry.id)
        // This assertion will now pass because both objects have millisecond precision.
        assertEquals(originalLog.symptomLogs.toSet(), retrievedLog.symptomLogs.toSet())
        assertEquals(originalLog.medicationLogs.toSet(), retrievedLog.medicationLogs.toSet())
    }

    @Test
    fun saveFullLog_WHEN_calledAgainForSameDay_THEN_updatesDataCorrectly() = runTest {
        // ARRANGE: Save an initial log
        val testDate = LocalDate(2025, 8, 15)
        val cycle = repository.startNewCycle(testDate.minus(5, DateTimeUnit.DAY))
        val symptom1 = repository.createOrGetSymptomInLibrary("Cramps", SymptomCategory.PAIN)
        val symptom2 = repository.createOrGetSymptomInLibrary("Headache", SymptomCategory.PAIN)
        val initialLog = FullDailyLog(
            entry = DailyEntry("entry-1", cycle.id, testDate, 6, createdAt = Clock.System.now(), updatedAt = Clock.System.now()),
            symptomLogs = listOf(SymptomLog("slog-1", "entry-1", symptom1.id, 3, Clock.System.now()))
        )
        repository.saveFullLog(initialLog)

        // Create an updated version of the log
        val updatedLog = initialLog.copy(
            symptomLogs = listOf(SymptomLog("slog-2", "entry-1", symptom2.id, 5, Clock.System.now()))
        )

        // ACT
        repository.saveFullLog(updatedLog)
        val retrievedLog = repository.getFullLogForDate(testDate)

        // ASSERT
        assertNotNull(retrievedLog)
        assertEquals(1, retrievedLog.symptomLogs.size)
        assertEquals(symptom2.id, retrievedLog.symptomLogs.first().symptomId, "The symptom list should have been replaced, not appended")
    }

    // --- Tests for Reactive Flows (getAllCycles, etc.) ---

    @Test
    fun getAllCycles_WHEN_databaseChanges_THEN_flowEmitsUpdatedList() = runTest {
        repository.getAllCycles().test {
            // 1. Initial emission is empty
            assertEquals(0, awaitItem().size, "Initially, list should be empty")

            // 2. Insert a cycle
            val cycle1 = repository.startNewCycle(LocalDate(2025, 1, 1))
            assertEquals(1, awaitItem().size, "After insert, list should have one cycle")

            // 3. Update the cycle
            repository.updateCycleEndDate(cycle1.id, LocalDate(2025, 1, 5))
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
        repository.createCompletedCycle(LocalDate(2025, 5, 10), LocalDate(2025, 5, 15))

        // ACT
        val isAvailable = repository.isDateRangeAvailable(
            startDate = LocalDate(2025, 5, 12),
            endDate = LocalDate(2025, 5, 17)
        )

        // ASSERT
        assertFalse(isAvailable)
    }
}