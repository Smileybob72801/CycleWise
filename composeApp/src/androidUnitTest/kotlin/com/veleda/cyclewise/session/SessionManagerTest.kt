package com.veleda.cyclewise.session

import android.content.Context
import android.util.Log
import com.veleda.cyclewise.androidData.local.database.PeriodDatabase
import com.veleda.cyclewise.androidData.local.database.RekeyVerificationFailedException
import com.veleda.cyclewise.androidData.local.draft.LockedWaterDraft
import com.veleda.cyclewise.di.SESSION_SCOPE
import com.veleda.cyclewise.domain.repository.PeriodRepository
import com.veleda.cyclewise.domain.services.PassphraseService
import com.veleda.cyclewise.settings.AppSettings
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.koin.java.KoinJavaComponent.getKoin
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Unit tests for [SessionManager] following the Given-When-Then convention.
 *
 * Uses a minimal Koin context with mocked session-scoped dependencies to verify
 * session lifecycle operations (open, close, isSessionActive, changePassphrase).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SessionManagerTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var mockAppSettings: AppSettings
    private lateinit var mockLockedWaterDraft: LockedWaterDraft
    private lateinit var mockPassphraseService: PassphraseService
    private lateinit var mockDb: PeriodDatabase
    private lateinit var mockRepository: PeriodRepository
    private lateinit var realFingerprintHolder: KeyFingerprintHolder

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        mockAppSettings = mockk(relaxed = true)
        mockLockedWaterDraft = mockk(relaxed = true)
        mockPassphraseService = mockk(relaxed = true)
        mockDb = mockk(relaxed = true)
        mockRepository = mockk(relaxed = true)
        realFingerprintHolder = KeyFingerprintHolder()

        // Stub android.util.Log
        mockkStatic(Log::class)
        every { Log.e(any(), any()) } returns 0
        every { Log.e(any(), any(), any()) } returns 0
        every { Log.i(any(), any()) } returns 0

        // Stub LockedWaterDraft.readAll to return empty map
        coEvery { mockLockedWaterDraft.readAll() } returns emptyMap()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkStatic(Log::class)
        try { stopKoin() } catch (_: Exception) { }
    }

    /**
     * Starts Koin with a session scope that provides mocked DB, repo, and fingerprint holder.
     * Returns a [SessionManager] wired to the test Koin context.
     */
    private fun startKoinAndCreateManager(): SessionManager {
        startKoin {
            modules(
                module {
                    single<PassphraseService> { mockPassphraseService }
                    single<Context> { mockk(relaxed = true) }

                    scope(SESSION_SCOPE) {
                        scoped { realFingerprintHolder }
                        scoped<PeriodDatabase> { mockDb }
                        scoped<PeriodRepository> { mockRepository }
                    }
                }
            )
        }
        return SessionManager(
            appSettings = mockAppSettings,
            lockedWaterDraft = mockLockedWaterDraft,
        )
    }

    // ── isSessionActive ──────────────────────────────────────────────

    @Test
    fun `isSessionActive WHEN no session exists THEN returns false`() {
        // GIVEN — Koin started but no session scope created
        val manager = startKoinAndCreateManager()

        // THEN — isSessionActive is false
        assertFalse(manager.isSessionActive)
    }

    @Test
    fun `isSessionActive WHEN session exists THEN returns true`() {
        // GIVEN — Koin started with a session scope
        val manager = startKoinAndCreateManager()
        getKoin().createScope("session", named("UnlockedSessionScope"))

        // THEN — isSessionActive is true
        assertTrue(manager.isSessionActive)
    }

    // ── closeSession ─────────────────────────────────────────────────

    @Test
    fun `closeSession WHEN no session exists THEN noOp`() {
        // GIVEN — Koin started but no session scope
        val manager = startKoinAndCreateManager()

        // WHEN — closeSession called
        manager.closeSession()

        // THEN — no exception (no-op)
        assertFalse(manager.isSessionActive)
    }

    @Test
    fun `closeSession WHEN session exists THEN closes it`() {
        // GIVEN — Koin started with a session scope
        val manager = startKoinAndCreateManager()
        val scope = getKoin().createScope("session", named("UnlockedSessionScope"))
        assertTrue(manager.isSessionActive)

        // WHEN — closeSession called
        manager.closeSession()

        // THEN — session is no longer active
        assertFalse(manager.isSessionActive)
        assertTrue(scope.closed)
    }

    // ── changePassphrase ─────────────────────────────────────────────

    @Test
    fun `changePassphrase WHEN no active session THEN returns Failed`() = runTest {
        // GIVEN — Koin started but no session scope
        val manager = startKoinAndCreateManager()

        // WHEN — changePassphrase called
        val result = manager.changePassphrase("current", "newpassphrase")

        // THEN — returns Failed
        assertEquals(ChangePassphraseResult.Failed, result)
    }

    @Test
    fun `changePassphrase WHEN wrong current passphrase THEN returns WrongCurrent`() = runTest {
        // GIVEN — session scope with fingerprint for correct key
        val manager = startKoinAndCreateManager()
        getKoin().createScope("session", named("UnlockedSessionScope"))
        realFingerprintHolder.store("correct-key-32-bytes-long-here!!".toByteArray())

        every { mockPassphraseService.deriveKey("wrong-current") } returns "wrong-key-not-matching-32bytes!".toByteArray()

        // WHEN — changePassphrase with wrong current
        val result = manager.changePassphrase("wrong-current", "newpassphrase123")

        // THEN — returns WrongCurrent
        assertEquals(ChangePassphraseResult.WrongCurrent, result)
    }

    @Test
    fun `changePassphrase WHEN correct current THEN rekeys and returns Success`() = runTest {
        // GIVEN — session scope with fingerprint for correct key
        val manager = startKoinAndCreateManager()
        getKoin().createScope("session", named("UnlockedSessionScope"))
        val correctKey = "correct-key-32-bytes-long-here!!".toByteArray()
        realFingerprintHolder.store(correctKey)

        every { mockPassphraseService.deriveKey("correct-current") } returns "correct-key-32-bytes-long-here!!".toByteArray()
        every { mockPassphraseService.deriveKey("newpassphrase123") } returns "new-key-32-bytes-long-here-now!!".toByteArray()
        every { mockDb.changeEncryptionKey(any(), any(), any()) } just runs

        // WHEN — changePassphrase with correct current
        val result = manager.changePassphrase("correct-current", "newpassphrase123")

        // THEN — returns Success and rekey was called
        assertEquals(ChangePassphraseResult.Success, result)
        verify(exactly = 1) { mockDb.changeEncryptionKey(any(), any(), any()) }

        // THEN — fingerprint is updated to new key
        assertTrue(realFingerprintHolder.matches("new-key-32-bytes-long-here-now!!".toByteArray()))
        assertFalse(realFingerprintHolder.matches("correct-key-32-bytes-long-here!!".toByteArray()))
    }

    @Test
    fun `changePassphrase WHEN rekeyVerificationFails THEN returns VerificationFailed`() = runTest {
        // GIVEN — session scope where rekey throws RekeyVerificationFailedException
        val manager = startKoinAndCreateManager()
        getKoin().createScope("session", named("UnlockedSessionScope"))
        realFingerprintHolder.store("correct-key-32-bytes-long-here!!".toByteArray())

        every { mockPassphraseService.deriveKey("correct-current") } returns "correct-key-32-bytes-long-here!!".toByteArray()
        every { mockPassphraseService.deriveKey("newpassphrase123") } returns "new-key-32-bytes-long-here-now!!".toByteArray()
        every { mockDb.changeEncryptionKey(any(), any(), any()) } throws
                RekeyVerificationFailedException("Rekey verification failed")

        // WHEN — changePassphrase with correct current
        val result = manager.changePassphrase("correct-current", "newpassphrase123")

        // THEN — returns VerificationFailed
        assertEquals(ChangePassphraseResult.VerificationFailed, result)
    }

    @Test
    fun `changePassphrase WHEN genericException THEN returns Failed`() = runTest {
        // GIVEN — session scope where rekey throws a generic exception
        val manager = startKoinAndCreateManager()
        getKoin().createScope("session", named("UnlockedSessionScope"))
        realFingerprintHolder.store("correct-key-32-bytes-long-here!!".toByteArray())

        every { mockPassphraseService.deriveKey("correct-current") } returns "correct-key-32-bytes-long-here!!".toByteArray()
        every { mockPassphraseService.deriveKey("newpassphrase123") } returns "new-key-32-bytes-long-here-now!!".toByteArray()
        every { mockDb.changeEncryptionKey(any(), any(), any()) } throws RuntimeException("unexpected")

        // WHEN — changePassphrase with correct current
        val result = manager.changePassphrase("correct-current", "newpassphrase123")

        // THEN — returns Failed
        assertEquals(ChangePassphraseResult.Failed, result)
    }
}
