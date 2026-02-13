package com.veleda.cyclewise.settings

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Unit tests for the log-summary-display preferences in [AppSettings].
 *
 * Because [preferencesDataStore] is a process-level singleton, tests verify
 * the set/read roundtrip rather than assuming a clean default state.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(application = com.veleda.cyclewise.RobolectricTestApp::class)
class AppSettingsTest {

    private lateinit var appSettings: AppSettings

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        appSettings = AppSettings(context)
    }

    // --- showMoodInSummary ---

    @Test
    fun setShowMoodInSummary_WHEN_setToFalse_THEN_emitsFalse() = runTest {
        // GIVEN a fresh toggle
        appSettings.setShowMoodInSummary(true)

        appSettings.showMoodInSummary.test {
            // consume the initial value
            assertTrue(awaitItem())

            // WHEN setting to false
            appSettings.setShowMoodInSummary(false)

            // THEN the flow emits false
            assertFalse(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun setShowMoodInSummary_WHEN_setToTrue_THEN_emitsTrue() = runTest {
        // GIVEN mood was disabled
        appSettings.setShowMoodInSummary(false)

        appSettings.showMoodInSummary.test {
            assertFalse(awaitItem())

            // WHEN re-enabling
            appSettings.setShowMoodInSummary(true)

            // THEN the flow emits true
            assertTrue(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    // --- showEnergyInSummary ---

    @Test
    fun setShowEnergyInSummary_WHEN_setToFalse_THEN_emitsFalse() = runTest {
        appSettings.setShowEnergyInSummary(true)

        appSettings.showEnergyInSummary.test {
            assertTrue(awaitItem())

            appSettings.setShowEnergyInSummary(false)

            assertFalse(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun setShowEnergyInSummary_WHEN_setToTrue_THEN_emitsTrue() = runTest {
        appSettings.setShowEnergyInSummary(false)

        appSettings.showEnergyInSummary.test {
            assertFalse(awaitItem())

            appSettings.setShowEnergyInSummary(true)

            assertTrue(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    // --- showLibidoInSummary ---

    @Test
    fun setShowLibidoInSummary_WHEN_setToFalse_THEN_emitsFalse() = runTest {
        appSettings.setShowLibidoInSummary(true)

        appSettings.showLibidoInSummary.test {
            assertTrue(awaitItem())

            appSettings.setShowLibidoInSummary(false)

            assertFalse(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun setShowLibidoInSummary_WHEN_setToTrue_THEN_emitsTrue() = runTest {
        appSettings.setShowLibidoInSummary(false)

        appSettings.showLibidoInSummary.test {
            assertFalse(awaitItem())

            appSettings.setShowLibidoInSummary(true)

            assertTrue(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }
}
