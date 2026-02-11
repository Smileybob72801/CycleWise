package com.veleda.cyclewise.ui.auth

import com.veleda.cyclewise.androidData.local.draft.LockedWaterDraft
import com.veleda.cyclewise.domain.models.WaterIntake
import com.veleda.cyclewise.domain.repository.PeriodRepository
import com.veleda.cyclewise.testutil.TestData
import android.util.Log
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkStatic
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WaterSyncTest {

    private val repository: PeriodRepository = mockk(relaxed = true)
    private val lockedWaterDraft: LockedWaterDraft = mockk(relaxed = true)
    private val syncer = WaterDraftSyncer(lockedWaterDraft, repository)

    @Before
    fun setUp() {
        mockkStatic(Log::class)
        every { Log.e(any(), any(), any()) } returns 0
    }

    @After
    fun tearDown() {
        unmockkStatic(Log::class)
    }

    private val today = LocalDate(2025, 6, 15)
    private val yesterday = LocalDate(2025, 6, 14)
    private val twoDaysAgo = LocalDate(2025, 6, 13)

    // --- Tests ---

    @Test
    fun sync_WHEN_dbEmpty_THEN_insertsAllDrafts() = runTest {
        // ARRANGE
        coEvery { lockedWaterDraft.readAll() } returns mapOf(yesterday to 5)
        coEvery { repository.getWaterIntakeForDates(any()) } returns emptyList()

        // ACT
        syncer.sync(today = today)

        // ASSERT
        val slot = slot<WaterIntake>()
        coVerify { repository.upsertWaterIntake(capture(slot)) }
        assertEquals(yesterday, slot.captured.date)
        assertEquals(5, slot.captured.cups)
        coVerify { lockedWaterDraft.clearDates(setOf(yesterday)) }
    }

    @Test
    fun sync_WHEN_draftHigherThanDb_THEN_writesToDb() = runTest {
        // ARRANGE
        coEvery { lockedWaterDraft.readAll() } returns mapOf(yesterday to 8)
        coEvery { repository.getWaterIntakeForDates(any()) } returns listOf(
            WaterIntake(yesterday, 5, TestData.INSTANT, TestData.INSTANT)
        )

        // ACT
        syncer.sync(today = today)

        // ASSERT
        val slot = slot<WaterIntake>()
        coVerify { repository.upsertWaterIntake(capture(slot)) }
        assertEquals(8, slot.captured.cups)
    }

    @Test
    fun sync_WHEN_dbHigherOrEqual_THEN_skipsWrite() = runTest {
        // ARRANGE
        coEvery { lockedWaterDraft.readAll() } returns mapOf(yesterday to 3)
        coEvery { repository.getWaterIntakeForDates(any()) } returns listOf(
            WaterIntake(yesterday, 5, TestData.INSTANT, TestData.INSTANT)
        )

        // ACT
        syncer.sync(today = today)

        // ASSERT
        coVerify(exactly = 0) { repository.upsertWaterIntake(any()) }
        coVerify { lockedWaterDraft.clearDates(setOf(yesterday)) }
    }

    @Test
    fun sync_WHEN_draftEmpty_THEN_noOp() = runTest {
        // ARRANGE
        coEvery { lockedWaterDraft.readAll() } returns emptyMap()

        // ACT
        syncer.sync(today = today)

        // ASSERT
        coVerify(exactly = 0) { repository.getWaterIntakeForDates(any()) }
        coVerify(exactly = 0) { repository.upsertWaterIntake(any()) }
        coVerify(exactly = 0) { lockedWaterDraft.clearDates(any()) }
    }

    @Test
    fun sync_WHEN_partialFailure_THEN_clearsOnlySuccessful() = runTest {
        // ARRANGE
        coEvery { lockedWaterDraft.readAll() } returns mapOf(yesterday to 5, twoDaysAgo to 3)
        coEvery { repository.getWaterIntakeForDates(any()) } returns emptyList()
        var callCount = 0
        coEvery { repository.upsertWaterIntake(any()) } answers {
            callCount++
            if (callCount == 2) throw RuntimeException("DB error")
        }

        // ACT
        syncer.sync(today = today)

        // ASSERT
        val clearedSlot = slot<Set<LocalDate>>()
        coVerify { lockedWaterDraft.clearDates(capture(clearedSlot)) }
        assertTrue(clearedSlot.captured.size < 2)
    }

    @Test
    fun sync_WHEN_multipleDates_THEN_clearsOnlyPastDates() = runTest {
        // ARRANGE
        coEvery { lockedWaterDraft.readAll() } returns mapOf(today to 5, yesterday to 3)
        coEvery { repository.getWaterIntakeForDates(any()) } returns emptyList()

        // ACT
        syncer.sync(today = today)

        // ASSERT
        val slot = slot<WaterIntake>()
        coVerify(exactly = 1) { repository.upsertWaterIntake(capture(slot)) }
        assertEquals(yesterday, slot.captured.date)
        assertEquals(3, slot.captured.cups)
        coVerify { lockedWaterDraft.clearDates(setOf(yesterday)) }
    }

    @Test
    fun sync_WHEN_onlyTodayDraft_THEN_noOp() = runTest {
        // ARRANGE
        coEvery { lockedWaterDraft.readAll() } returns mapOf(today to 4)

        // ACT
        syncer.sync(today = today)

        // ASSERT
        coVerify(exactly = 0) { repository.getWaterIntakeForDates(any()) }
        coVerify(exactly = 0) { repository.upsertWaterIntake(any()) }
        coVerify(exactly = 0) { lockedWaterDraft.clearDates(any()) }
    }
}
