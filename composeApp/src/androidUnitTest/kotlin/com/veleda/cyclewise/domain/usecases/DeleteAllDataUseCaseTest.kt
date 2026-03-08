package com.veleda.cyclewise.domain.usecases

import android.content.Context
import com.veleda.cyclewise.androidData.local.draft.LockedWaterDraft
import com.veleda.cyclewise.reminders.ReminderScheduler
import com.veleda.cyclewise.services.SaltStorage
import com.veleda.cyclewise.settings.AppSettings
import com.veleda.cyclewise.ui.coachmark.HintPreferences
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [DeleteAllDataUseCase] following the Given-When-Then convention.
 *
 * Mocks all six dependencies and verifies that [invoke] calls every clear/delete method.
 */
class DeleteAllDataUseCaseTest {

    private lateinit var mockContext: Context
    private lateinit var mockAppSettings: AppSettings
    private lateinit var mockSaltStorage: SaltStorage
    private lateinit var mockHintPreferences: HintPreferences
    private lateinit var mockLockedWaterDraft: LockedWaterDraft
    private lateinit var mockReminderScheduler: ReminderScheduler

    private lateinit var useCase: DeleteAllDataUseCase

    @Before
    fun setUp() {
        mockContext = mockk(relaxed = true)
        mockAppSettings = mockk(relaxed = true)
        mockSaltStorage = mockk(relaxed = true)
        mockHintPreferences = mockk(relaxed = true)
        mockLockedWaterDraft = mockk(relaxed = true)
        mockReminderScheduler = mockk(relaxed = true)

        every { mockContext.deleteDatabase(any()) } returns true

        useCase = DeleteAllDataUseCase(
            context = mockContext,
            appSettings = mockAppSettings,
            saltStorage = mockSaltStorage,
            hintPreferences = mockHintPreferences,
            lockedWaterDraft = mockLockedWaterDraft,
            reminderScheduler = mockReminderScheduler,
        )
    }

    @Test
    fun `invoke WHEN called THEN deletesDatabase`() = runTest {
        // GIVEN — use case with mocked dependencies
        // WHEN — use case is invoked
        useCase()

        // THEN — database is deleted with the correct file name
        verify(exactly = 1) { mockContext.deleteDatabase("cyclewise.db") }
    }

    @Test
    fun `invoke WHEN called THEN clearsAppSettings`() = runTest {
        // GIVEN — use case with mocked dependencies
        // WHEN — use case is invoked
        useCase()

        // THEN — app settings are cleared
        coVerify(exactly = 1) { mockAppSettings.clearAll() }
    }

    @Test
    fun `invoke WHEN called THEN clearsSaltStorage`() = runTest {
        // GIVEN — use case with mocked dependencies
        // WHEN — use case is invoked
        useCase()

        // THEN — salt storage is cleared
        verify(exactly = 1) { mockSaltStorage.clear() }
    }

    @Test
    fun `invoke WHEN called THEN resetsHintPreferences`() = runTest {
        // GIVEN — use case with mocked dependencies
        // WHEN — use case is invoked
        useCase()

        // THEN — hint preferences are reset
        coVerify(exactly = 1) { mockHintPreferences.resetAll() }
    }

    @Test
    fun `invoke WHEN called THEN clearsLockedWaterDraft`() = runTest {
        // GIVEN — use case with mocked dependencies
        // WHEN — use case is invoked
        useCase()

        // THEN — locked water draft is cleared
        coVerify(exactly = 1) { mockLockedWaterDraft.clearAll() }
    }

    @Test
    fun `invoke WHEN called THEN cancelsAllReminders`() = runTest {
        // GIVEN — use case with mocked dependencies
        // WHEN — use case is invoked
        useCase()

        // THEN — all reminders are cancelled
        verify(exactly = 1) { mockReminderScheduler.cancelAll() }
    }

    @Test
    fun `invoke WHEN called THEN callsAllSixClearMethods`() = runTest {
        // GIVEN — use case with mocked dependencies
        // WHEN — use case is invoked
        useCase()

        // THEN — all six wipe operations are called
        verify(exactly = 1) { mockContext.deleteDatabase("cyclewise.db") }
        coVerify(exactly = 1) { mockAppSettings.clearAll() }
        verify(exactly = 1) { mockSaltStorage.clear() }
        coVerify(exactly = 1) { mockHintPreferences.resetAll() }
        coVerify(exactly = 1) { mockLockedWaterDraft.clearAll() }
        verify(exactly = 1) { mockReminderScheduler.cancelAll() }
    }
}
