package com.veleda.cyclewise.androidData.repository

import app.cash.turbine.test
import com.benasher44.uuid.uuid4
import com.veleda.cyclewise.KoinTestRule
import com.veleda.cyclewise.androidData.local.database.PeriodDatabase
import com.veleda.cyclewise.domain.models.*
import com.veleda.cyclewise.domain.repository.PeriodRepository
import com.veleda.cyclewise.testutil.TestData
import com.veleda.cyclewise.testutil.testDatabaseModule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.*
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.inject
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.*

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE)
class RoomCycleRepositoryTest : KoinTest {

    private val repositoryModule = module {
        includes(testDatabaseModule)
        single<PeriodRepository> {
            RoomPeriodRepository(
                db = get(),
                periodDao = get(),
                dailyEntryDao = get(),
                symptomDao = get(),
                symptomLogDao = get(),
                medicationDao = get(),
                medicationLogDao = get(),
                periodLogDao = get(),
                waterIntakeDao = get()
            )
        }
    }

    @get:Rule
    val koinRule = KoinTestRule(listOf(repositoryModule))

    private val repository: PeriodRepository by inject()
    private val db: PeriodDatabase by inject()

    @After
    fun tearDown() {
        db.close()
    }

    // --- Test Data Helpers ---
    private val testDate = LocalDate(2025, 8, 15)
    private val testNow = TestData.INSTANT

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
    fun saveFullLog_WHEN_fullLogSaved_THEN_retrievesAllDataCorrectly() = runTest {
        // ARRANGE
        repository.startNewPeriod(testDate.minus(5, DateTimeUnit.DAY))
        val symptom1 = repository.createOrGetSymptomInLibrary("Cramps", SymptomCategory.PAIN)
        val medication1 = repository.createOrGetMedicationInLibrary("Ibuprofen")
        val baseEntry = createBaseDailyEntry("entry-1", testDate)
        val originalLog = FullDailyLog(
            entry = baseEntry,
            periodLog = createHeavyPeriodLog("entry-1"),
            symptomLogs = listOf(
                SymptomLog("slog-1", "entry-1", symptom1.id, 4, testNow)
            ),
            medicationLogs = listOf(
                MedicationLog("mlog-1", "entry-1", medication1.id, testNow)
            )
        )

        // ACT
        repository.saveFullLog(originalLog)
        val retrievedLog = repository.getFullLogForDate(testDate)

        // ASSERT
        assertNotNull(retrievedLog)
        assertEquals(originalLog.entry.id, retrievedLog.entry.id)
        assertNotNull(retrievedLog.periodLog, "PeriodLog should be retrieved.")
        assertEquals(FlowIntensity.HEAVY, retrievedLog.periodLog!!.flowIntensity)
        assertEquals(originalLog.symptomLogs.toSet(), retrievedLog.symptomLogs.toSet())
        assertEquals(originalLog.medicationLogs.toSet(), retrievedLog.medicationLogs.toSet())
    }

    @Test
    fun saveFullLog_WHEN_FlowIsCleared_THEN_PeriodLogIsDeleted() = runTest {
        // ARRANGE
        repository.startNewPeriod(testDate.minus(5, DateTimeUnit.DAY))
        val initialLog = FullDailyLog(
            entry = createBaseDailyEntry("entry-1", testDate),
            periodLog = createHeavyPeriodLog("entry-1"),
        )
        repository.saveFullLog(initialLog)
        assertNotNull(repository.getFullLogForDate(testDate)?.periodLog, "Precondition: PeriodLog should exist")

        // ACT
        val clearedLog = initialLog.copy(periodLog = null)
        repository.saveFullLog(clearedLog)

        // ASSERT
        val retrievedLog = repository.getFullLogForDate(testDate)
        assertNotNull(retrievedLog)
        assertNull(retrievedLog.periodLog, "PeriodLog should be null after clearing flow data.")
    }

    @Test
    fun saveFullLog_WHEN_calledAgainForSameDay_THEN_updatesDataCorrectly() = runTest {
        // ARRANGE
        repository.startNewPeriod(testDate.minus(5, DateTimeUnit.DAY))
        val symptom1 = repository.createOrGetSymptomInLibrary("Cramps", SymptomCategory.PAIN)
        val symptom2 = repository.createOrGetSymptomInLibrary("Headache", SymptomCategory.PAIN)
        val initialLog = FullDailyLog(
            entry = createBaseDailyEntry("entry-1", testDate),
            periodLog = createHeavyPeriodLog("entry-1"),
            symptomLogs = listOf(SymptomLog("slog-1", "entry-1", symptom1.id, 3, testNow))
        )
        repository.saveFullLog(initialLog)
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

    // --- Tests for Reactive Flows ---

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun getAllPeriods_WHEN_databaseChanges_THEN_flowEmitsUpdatedList() = runTest {
        repository.getAllPeriods().test {
            // ASSERT - initial emission is empty
            assertEquals(0, awaitItem().size, "Initially, list should be empty")

            // ACT - insert a cycle
            val cycle1 = repository.startNewPeriod(LocalDate(2025, 1, 1))
            advanceUntilIdle()

            // ASSERT
            assertEquals(1, awaitItem().size, "After insert, list should have one cycle")

            // ACT - update the cycle
            repository.updatePeriodEndDate(cycle1.id, LocalDate(2025, 1, 5))
            advanceUntilIdle()

            // ASSERT
            val updatedList = awaitItem()
            assertEquals(1, updatedList.size)
            assertEquals(LocalDate(2025, 1, 5), updatedList.first().endDate)
        }
    }

    // --- Tests for Library Get-Or-Create Logic ---

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun createOrGetSymptomInLibrary_WHEN_symptomExists_THEN_returnsExistingAndDoesNotChangeCategory() = runTest {
        // ARRANGE
        val symptom1 = repository.createOrGetSymptomInLibrary("Headache", SymptomCategory.PAIN)

        // ACT
        val symptom2 = repository.createOrGetSymptomInLibrary("Headache", SymptomCategory.MOOD)

        // ASSERT
        assertEquals(symptom1.id, symptom2.id, "Should return the existing symptom, not create a new one")
        advanceUntilIdle()

        repository.getSymptomLibrary().test {
            val libraryList = awaitItem()
            val headacheInDb = libraryList.firstOrNull { it.id == symptom1.id }
            assertNotNull(headacheInDb)
            assertEquals(SymptomCategory.PAIN, headacheInDb.category, "The category of the original symptom should not have been changed")
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
        repository.startNewPeriod(LocalDate(2025, 1, 1))
        repository.createCompletedPeriod(LocalDate(2025, 2, 1), LocalDate(2025, 2, 28))
        val symptom1 = repository.createOrGetSymptomInLibrary("Headache", SymptomCategory.PAIN)

        val log1 = FullDailyLog(DailyEntry("entry-1", LocalDate(2025, 1, 5), 5, createdAt = testNow, updatedAt = testNow), symptomLogs = listOf(SymptomLog("slog-1", "entry-1", symptom1.id, 3, testNow)))
        val log2 = FullDailyLog(DailyEntry("entry-2", LocalDate(2025, 2, 5), 5, createdAt = testNow, updatedAt = testNow), symptomLogs = listOf(SymptomLog("slog-2", "entry-2", symptom1.id, 4, testNow)))
        val log3WithNoSymptoms = FullDailyLog(DailyEntry("entry-3", LocalDate(2025, 2, 6), 6, createdAt = testNow, updatedAt = testNow))

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
        repository.startNewPeriod(LocalDate(2025, 1, 1))
        val med1 = repository.createOrGetMedicationInLibrary("Ibuprofen")

        val log1 = FullDailyLog(DailyEntry("entry-1", LocalDate(2025, 1, 5), 5, createdAt = testNow, updatedAt = testNow), medicationLogs = listOf(MedicationLog("mlog-1", "entry-1", med1.id, testNow)))
        repository.saveFullLog(log1)

        // ACT
        val allLogs = repository.getAllMedicationLogs()

        // ASSERT
        assertEquals(1, allLogs.size)
        assertEquals("mlog-1", allLogs.first().id)
    }
}
