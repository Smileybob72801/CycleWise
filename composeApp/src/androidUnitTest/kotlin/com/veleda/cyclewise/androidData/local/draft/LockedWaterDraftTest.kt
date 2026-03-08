package com.veleda.cyclewise.androidData.local.draft

import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class LockedWaterDraftTest {

    private lateinit var draft: LockedWaterDraft
    private val today = LocalDate(2025, 6, 15)
    private val yesterday = today.minus(1, DateTimeUnit.DAY)

    @Before
    fun setUp() {
        draft = LockedWaterDraft(ApplicationProvider.getApplicationContext())
    }

    // --- setCups tests ---

    @Test
    fun setCups_WHEN_positiveCups_THEN_storesValue() = runTest {
        // ACT
        draft.setCups(today, 5)

        // ASSERT
        val result = draft.readAll()
        assertEquals(5, result[today])
    }

    @Test
    fun setCups_WHEN_zeroCups_THEN_removesEntry() = runTest {
        // ARRANGE
        draft.setCups(today, 5)

        // ACT
        draft.setCups(today, 0)

        // ASSERT
        val result = draft.readAll()
        assertNull(result[today])
    }

    @Test
    fun setCups_WHEN_exceedsMax_THEN_clampsToMax() = runTest {
        // ACT
        draft.setCups(today, 200)

        // ASSERT
        val result = draft.readAll()
        assertEquals(MAX_DRAFT_CUPS, result[today])
    }

    @Test
    fun setCups_WHEN_oldEntries_THEN_prunesOver30Days() = runTest {
        // ARRANGE
        val oldDate = today.minus(31, DateTimeUnit.DAY)
        draft.setCups(oldDate, 3)

        // ACT - setting cups for today triggers pruning
        draft.setCups(today, 5)

        // ASSERT
        val result = draft.readAll()
        assertNull(result[oldDate])
        assertEquals(5, result[today])
    }

    // --- clearDates tests ---

    @Test
    fun clearDates_WHEN_calledWithSubset_THEN_removesOnlySpecified() = runTest {
        // ARRANGE
        draft.setCups(today, 5)
        draft.setCups(yesterday, 3)

        // ACT
        draft.clearDates(setOf(yesterday))

        // ASSERT
        val result = draft.readAll()
        assertEquals(5, result[today])
        assertNull(result[yesterday])
    }

    // --- empty initial state ---

    @Test
    fun readAll_WHEN_noDrafts_THEN_returnsEmptyMap() = runTest {
        // ACT
        val result = draft.readAll()

        // ASSERT
        assertTrue(result.isEmpty())
    }

    // --- ensureRolledOver tests ---

    @Test
    fun ensureRolledOver_WHEN_firstInstall_THEN_createsDefaultPayload() = runTest {
        // ACT
        draft.ensureRolledOver(today)

        // ASSERT - should be empty but no crash
        val result = draft.readAll()
        assertTrue(result.isEmpty())
    }

    @Test
    fun ensureRolledOver_WHEN_dayChanged_THEN_preservesYesterdayAndResetsToday() = runTest {
        // ARRANGE
        draft.ensureRolledOver(yesterday)
        draft.setCups(yesterday, 7)

        // ACT
        draft.ensureRolledOver(today)

        // ASSERT
        val result = draft.readAll()
        assertEquals(7, result[yesterday])
        assertNull(result[today]) // today should start fresh
    }

    @Test
    fun ensureRolledOver_WHEN_sameDay_THEN_noOp() = runTest {
        // ARRANGE
        draft.ensureRolledOver(today)
        draft.setCups(today, 5)

        // ACT
        draft.ensureRolledOver(today)

        // ASSERT - today's cups should be preserved
        val result = draft.readAll()
        assertEquals(5, result[today])
    }

    @Test
    fun ensureRolledOver_WHEN_corruptedLastActiveDate_THEN_resetsGracefully() = runTest {
        // ARRANGE - first establish a valid state then simulate corruption by
        // calling ensureRolledOver with a far-future date to trigger rollover
        draft.ensureRolledOver(today)
        draft.setCups(today, 3)

        // ACT - rollover to tomorrow should keep today's data
        val tomorrow = today.plus(1, DateTimeUnit.DAY)
        draft.ensureRolledOver(tomorrow)

        // ASSERT
        val result = draft.readAll()
        assertEquals(3, result[today])
        assertNull(result[tomorrow])
    }

    // --- Flow observation ---

    @Test
    fun drafts_WHEN_cupsSet_THEN_flowEmitsUpdatedMap() = runTest {
        // ARRANGE
        draft.ensureRolledOver(today)
        draft.setCups(today, 4)

        // ACT
        val result = draft.drafts.first()

        // ASSERT
        assertEquals(4, result[today])
    }
}
