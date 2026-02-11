package com.veleda.cyclewise.ui.auth

import com.veleda.cyclewise.androidData.local.draft.LockedWaterDraft
import com.veleda.cyclewise.domain.models.WaterIntake
import com.veleda.cyclewise.domain.repository.PeriodRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Clock

class WaterSyncTest {

    private val repository: PeriodRepository = mockk(relaxed = true)
    private val lockedWaterDraft: LockedWaterDraft = mockk(relaxed = true)

    private val today = LocalDate(2025, 6, 15)
    private val yesterday = LocalDate(2025, 6, 14)
    private val twoDaysAgo = LocalDate(2025, 6, 13)

    /**
     * Simulates the sync logic from PassphraseViewModel.syncWaterDrafts
     * to test the merge strategy in isolation.
     */
    private suspend fun syncWaterDrafts() {
        val allDrafts = lockedWaterDraft.readAll()
        val drafts = allDrafts.filterKeys { it != today }
        if (drafts.isEmpty()) return

        val existing = repository.getWaterIntakeForDates(drafts.keys.toList())
            .associateBy { it.date }
        val now = Clock.System.now()
        val syncedDates = mutableSetOf<LocalDate>()

        for ((date, draftCups) in drafts) {
            try {
                val dbEntry = existing[date]
                val dbCups = dbEntry?.cups ?: 0
                if (draftCups > dbCups) {
                    repository.upsertWaterIntake(
                        WaterIntake(
                            date = date,
                            cups = draftCups,
                            createdAt = dbEntry?.createdAt ?: now,
                            updatedAt = now
                        )
                    )
                }
                syncedDates += date
            } catch (_: Exception) {
                // Don't add to syncedDates — will retry on next unlock
            }
        }

        if (syncedDates.isNotEmpty()) {
            lockedWaterDraft.clearDates(syncedDates)
        }
    }

    // --- Tests ---

    @Test
    fun sync_WHEN_dbEmpty_THEN_insertsAllDrafts() = runTest {
        // ARRANGE
        coEvery { lockedWaterDraft.readAll() } returns mapOf(yesterday to 5)
        coEvery { repository.getWaterIntakeForDates(any()) } returns emptyList()

        // ACT
        syncWaterDrafts()

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
        val now = Clock.System.now()
        coEvery { lockedWaterDraft.readAll() } returns mapOf(yesterday to 8)
        coEvery { repository.getWaterIntakeForDates(any()) } returns listOf(
            WaterIntake(yesterday, 5, now, now)
        )

        // ACT
        syncWaterDrafts()

        // ASSERT
        val slot = slot<WaterIntake>()
        coVerify { repository.upsertWaterIntake(capture(slot)) }
        assertEquals(8, slot.captured.cups)
    }

    @Test
    fun sync_WHEN_dbHigherOrEqual_THEN_skipsWrite() = runTest {
        // ARRANGE
        val now = Clock.System.now()
        coEvery { lockedWaterDraft.readAll() } returns mapOf(yesterday to 3)
        coEvery { repository.getWaterIntakeForDates(any()) } returns listOf(
            WaterIntake(yesterday, 5, now, now)
        )

        // ACT
        syncWaterDrafts()

        // ASSERT
        coVerify(exactly = 0) { repository.upsertWaterIntake(any()) }
        // Still clears the date since it was "processed" (db already has higher)
        coVerify { lockedWaterDraft.clearDates(setOf(yesterday)) }
    }

    @Test
    fun sync_WHEN_draftEmpty_THEN_noOp() = runTest {
        // ARRANGE
        coEvery { lockedWaterDraft.readAll() } returns emptyMap()

        // ACT
        syncWaterDrafts()

        // ASSERT
        coVerify(exactly = 0) { repository.getWaterIntakeForDates(any()) }
        coVerify(exactly = 0) { repository.upsertWaterIntake(any()) }
        coVerify(exactly = 0) { lockedWaterDraft.clearDates(any()) }
    }

    @Test
    fun sync_WHEN_partialFailure_THEN_clearsOnlySuccessful() = runTest {
        // ARRANGE
        val now = Clock.System.now()
        coEvery { lockedWaterDraft.readAll() } returns mapOf(yesterday to 5, twoDaysAgo to 3)
        coEvery { repository.getWaterIntakeForDates(any()) } returns emptyList()
        // First call succeeds, second throws
        var callCount = 0
        coEvery { repository.upsertWaterIntake(any()) } answers {
            callCount++
            if (callCount == 2) throw RuntimeException("DB error")
        }

        // ACT
        syncWaterDrafts()

        // ASSERT - only the successfully synced date should be cleared
        val clearedSlot = slot<Set<LocalDate>>()
        coVerify { lockedWaterDraft.clearDates(capture(clearedSlot)) }
        // The first date in iteration succeeded, the second failed
        assertTrue(clearedSlot.captured.size < 2)
    }

    @Test
    fun sync_WHEN_multipleDates_THEN_clearsOnlyPastDates() = runTest {
        // ARRANGE — drafts for today + yesterday; only yesterday should sync
        coEvery { lockedWaterDraft.readAll() } returns mapOf(today to 5, yesterday to 3)
        coEvery { repository.getWaterIntakeForDates(any()) } returns emptyList()

        // ACT
        syncWaterDrafts()

        // ASSERT — today excluded from sync: only yesterday written and cleared
        val slot = slot<WaterIntake>()
        coVerify(exactly = 1) { repository.upsertWaterIntake(capture(slot)) }
        assertEquals(yesterday, slot.captured.date)
        assertEquals(3, slot.captured.cups)
        coVerify { lockedWaterDraft.clearDates(setOf(yesterday)) }
    }

    @Test
    fun sync_WHEN_onlyTodayDraft_THEN_noOp() = runTest {
        // ARRANGE — only today's draft present; should be skipped entirely
        coEvery { lockedWaterDraft.readAll() } returns mapOf(today to 4)

        // ACT
        syncWaterDrafts()

        // ASSERT — no DB access, no clears
        coVerify(exactly = 0) { repository.getWaterIntakeForDates(any()) }
        coVerify(exactly = 0) { repository.upsertWaterIntake(any()) }
        coVerify(exactly = 0) { lockedWaterDraft.clearDates(any()) }
    }
}
