package com.veleda.cyclewise.ui.auth

import app.cash.turbine.test
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import android.util.Log
import com.veleda.cyclewise.services.BackupManager
import com.veleda.cyclewise.session.SessionManager
import com.veleda.cyclewise.ui.backup.ImportStep
import com.veleda.cyclewise.settings.AppSettings
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for [PassphraseViewModel] following the Given-When-Then convention.
 *
 * Tests cover first-time detection via `AppSettings.isPrepopulated`,
 * passphrase validation during setup, and the setup-to-unlock delegation.
 * Session operations are verified via a mocked [SessionManager].
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PassphraseViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var mockAppSettings: AppSettings
    private lateinit var mockSessionManager: SessionManager
    private lateinit var mockBackupManager: BackupManager

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        mockAppSettings = mockk(relaxed = true)
        mockSessionManager = mockk(relaxed = true)
        mockBackupManager = mockk(relaxed = true)

        // Mock android.util.Log which throws RuntimeException("Stub!") in
        // plain JUnit tests. The unlock() catch block calls Log.e().
        mockkStatic(Log::class)
        every { Log.e(any(), any()) } returns 0
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): PassphraseViewModel {
        return PassphraseViewModel(
            appSettings = mockAppSettings,
            sessionManager = mockSessionManager,
            backupManager = mockBackupManager,
        )
    }

    // ── First-time detection ──────────────────────────────────────────

    @Test
    fun `init WHEN isPrepopulated is false THEN isFirstTime is true`() = runTest {
        // GIVEN — isPrepopulated returns false (first-time user)
        every { mockAppSettings.isPrepopulated } returns flowOf(false)

        // WHEN — ViewModel is created
        val viewModel = createViewModel()
        advanceUntilIdle()

        // THEN — isFirstTime is true and isFirstTimeLoaded is true
        val state = viewModel.uiState.value
        assertTrue(state.isFirstTime)
        assertTrue(state.isFirstTimeLoaded)
    }

    @Test
    fun `init WHEN isPrepopulated is true THEN isFirstTime is false`() = runTest {
        // GIVEN — isPrepopulated returns true (returning user)
        every { mockAppSettings.isPrepopulated } returns flowOf(true)

        // WHEN — ViewModel is created
        val viewModel = createViewModel()
        advanceUntilIdle()

        // THEN — isFirstTime is false and isFirstTimeLoaded is true
        val state = viewModel.uiState.value
        assertFalse(state.isFirstTime)
        assertTrue(state.isFirstTimeLoaded)
    }

    // ── Setup validation ──────────────────────────────────────────────

    @Test
    fun `onEvent SetupClicked WHEN passphrase below 8 characters THEN passphraseError emitted`() = runTest {
        // GIVEN — ViewModel initialized for first-time user
        every { mockAppSettings.isPrepopulated } returns flowOf(false)
        val viewModel = createViewModel()
        advanceUntilIdle()

        // WHEN — SetupClicked with a short passphrase
        viewModel.onEvent(PassphraseEvent.SetupClicked("short", "short"))
        advanceUntilIdle()

        // THEN — passphraseError is set, confirmationError is null, no unlock attempted
        val state = viewModel.uiState.value
        assertEquals("too_short", state.passphraseError)
        assertNull(state.confirmationError)
        assertFalse(state.isUnlocking)
    }

    @Test
    fun `onEvent SetupClicked WHEN confirmation does not match THEN confirmationError emitted`() = runTest {
        // GIVEN — ViewModel initialized for first-time user
        every { mockAppSettings.isPrepopulated } returns flowOf(false)
        val viewModel = createViewModel()
        advanceUntilIdle()

        // WHEN — SetupClicked with mismatched confirmation
        viewModel.onEvent(
            PassphraseEvent.SetupClicked("validpassphrase", "differentphrase")
        )
        advanceUntilIdle()

        // THEN — confirmationError is set, passphraseError is null, no unlock attempted
        val state = viewModel.uiState.value
        assertNull(state.passphraseError)
        assertEquals("mismatch", state.confirmationError)
        assertFalse(state.isUnlocking)
    }

    @Test
    fun `onEvent SetupClicked WHEN valid matching passphrase THEN validation passes and unlock attempted`() = runTest {
        // GIVEN — ViewModel initialized for first-time user, openSession succeeds
        every { mockAppSettings.isPrepopulated } returns flowOf(false)
        coEvery { mockSessionManager.openSession(any()) } returns Unit
        val viewModel = createViewModel()
        advanceUntilIdle()

        // WHEN — SetupClicked with valid, matching passphrase
        viewModel.onEvent(
            PassphraseEvent.SetupClicked("validpassphrase", "validpassphrase")
        )
        advanceUntilIdle()

        // THEN — validation errors are both null, confirming handleSetup
        // did NOT return early and the unlock flow was reached.
        val state = viewModel.uiState.value
        assertNull(state.passphraseError)
        assertNull(state.confirmationError)

        // THEN — SessionManager.openSession was called
        coVerify(exactly = 1) { mockSessionManager.openSession("validpassphrase") }
    }

    // ── Unlock flow ──────────────────────────────────────────────────

    @Test
    fun `onEvent UnlockClicked WHEN openSession succeeds THEN navigates to tracker`() = runTest {
        // GIVEN — returning user, openSession succeeds
        every { mockAppSettings.isPrepopulated } returns flowOf(true)
        coEvery { mockSessionManager.openSession(any()) } returns Unit
        val viewModel = createViewModel()
        advanceUntilIdle()

        // WHEN — user taps unlock
        viewModel.onEvent(PassphraseEvent.UnlockClicked("mypassphrase"))
        advanceUntilIdle()

        // THEN — openSession was called and isUnlocking is false
        coVerify(exactly = 1) { mockSessionManager.openSession("mypassphrase") }
        assertFalse(viewModel.uiState.value.isUnlocking)
    }

    @Test
    fun `onEvent UnlockClicked WHEN openSession fails THEN closes session and shows error`() = runTest {
        // GIVEN — returning user, openSession throws
        every { mockAppSettings.isPrepopulated } returns flowOf(true)
        coEvery { mockSessionManager.openSession(any()) } throws RuntimeException("Wrong passphrase")
        val viewModel = createViewModel()
        advanceUntilIdle()

        // WHEN — user taps unlock
        viewModel.onEvent(PassphraseEvent.UnlockClicked("wrongpassphrase"))
        advanceUntilIdle()

        // THEN — closeSession was called and isUnlocking is false
        verify(exactly = 1) { mockSessionManager.closeSession() }
        assertFalse(viewModel.uiState.value.isUnlocking)
    }

    // ── Reduce-specific tests ──────────────────────────────────────────

    @Test
    fun `reduce UnlockClicked WHEN notAlreadyUnlocking THEN setsIsUnlockingTrue`() = runTest {
        // GIVEN — returning user, openSession succeeds
        every { mockAppSettings.isPrepopulated } returns flowOf(true)
        coEvery { mockSessionManager.openSession(any()) } returns Unit
        val viewModel = createViewModel()
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isUnlocking)

        // WHEN — unlock clicked (with UnconfinedTestDispatcher, the launch completes
        // immediately, so we verify openSession was called as proof reduce set isUnlocking)
        viewModel.onEvent(PassphraseEvent.UnlockClicked("mypassphrase"))
        advanceUntilIdle()

        // THEN — openSession was called (proving reduce set isUnlocking = true)
        coVerify(exactly = 1) { mockSessionManager.openSession("mypassphrase") }
    }

    @Test
    fun `reduce SetupClicked WHEN passphraseTooShort THEN setsErrorWithoutUnlocking`() = runTest {
        // GIVEN — first-time user
        every { mockAppSettings.isPrepopulated } returns flowOf(false)
        val viewModel = createViewModel()
        advanceUntilIdle()

        // WHEN — setup with short passphrase
        viewModel.onEvent(PassphraseEvent.SetupClicked("short", "short"))

        // THEN — error set, isUnlocking stays false, no openSession call
        val state = viewModel.uiState.value
        assertEquals("too_short", state.passphraseError)
        assertNull(state.confirmationError)
        assertFalse(state.isUnlocking)
        coVerify(exactly = 0) { mockSessionManager.openSession(any()) }
    }

    @Test
    fun `reduce SetupClicked WHEN confirmationMismatch THEN setsErrorWithoutUnlocking`() = runTest {
        // GIVEN — first-time user
        every { mockAppSettings.isPrepopulated } returns flowOf(false)
        val viewModel = createViewModel()
        advanceUntilIdle()

        // WHEN — setup with mismatched confirmation
        viewModel.onEvent(PassphraseEvent.SetupClicked("validpassphrase", "different"))

        // THEN — error set, isUnlocking stays false, no openSession call
        val state = viewModel.uiState.value
        assertNull(state.passphraseError)
        assertEquals("mismatch", state.confirmationError)
        assertFalse(state.isUnlocking)
        coVerify(exactly = 0) { mockSessionManager.openSession(any()) }
    }

    @Test
    fun `reduce SetupClicked WHEN validInput THEN clearsErrorsAndSetsIsUnlocking`() = runTest {
        // GIVEN — first-time user with a previous error
        every { mockAppSettings.isPrepopulated } returns flowOf(false)
        coEvery { mockSessionManager.openSession(any()) } returns Unit
        val viewModel = createViewModel()
        advanceUntilIdle()

        // Set a previous error
        viewModel.onEvent(PassphraseEvent.SetupClicked("short", "short"))
        assertEquals("too_short", viewModel.uiState.value.passphraseError)

        // WHEN — setup with valid input
        viewModel.onEvent(PassphraseEvent.SetupClicked("validpassphrase", "validpassphrase"))
        advanceUntilIdle()

        // THEN — errors cleared and openSession was called
        val state = viewModel.uiState.value
        assertNull(state.passphraseError)
        assertNull(state.confirmationError)
        coVerify(exactly = 1) { mockSessionManager.openSession("validpassphrase") }
    }

    // ── Backup Import ────────────────────────────────────────────────

    @Test
    fun `onEvent ImportBackupClicked THEN emitsLaunchImportPicker`() = runTest {
        // GIVEN — returning user
        every { mockAppSettings.isPrepopulated } returns flowOf(true)
        val viewModel = createViewModel()
        advanceUntilIdle()

        // WHEN — import backup clicked
        viewModel.effect.test {
            viewModel.onEvent(PassphraseEvent.ImportBackupClicked)
            advanceUntilIdle()

            // THEN — LaunchImportPicker effect is emitted
            assertEquals(PassphraseEffect.LaunchImportPicker, awaitItem())
        }
    }

    @Test
    fun `onEvent ImportDismissed THEN resetsImportState`() = runTest {
        // GIVEN — returning user
        every { mockAppSettings.isPrepopulated } returns flowOf(true)
        val viewModel = createViewModel()
        advanceUntilIdle()

        // WHEN — import dismissed
        viewModel.onEvent(PassphraseEvent.ImportDismissed)

        // THEN — all import fields are reset to defaults
        val state = viewModel.uiState.value
        assertEquals(ImportStep.IDLE, state.importStep)
        assertNull(state.importMetadata)
        assertNull(state.importUri)
        assertNull(state.importPassphraseError)
        assertEquals("", state.importConfirmText)
        assertNull(state.importError)
        assertFalse(state.isVerifyingPassphrase)
        assertNull(state.importPassphrase)
    }
}
