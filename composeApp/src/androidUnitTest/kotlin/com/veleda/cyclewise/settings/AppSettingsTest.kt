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
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Unit tests for the log-summary-display, phase-visibility, and phase-color preferences
 * in [AppSettings].
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

    // --- showFollicularPhase ---

    @Test
    fun setShowFollicularPhase_WHEN_setToFalse_THEN_emitsFalse() = runTest {
        appSettings.setShowFollicularPhase(true)

        appSettings.showFollicularPhase.test {
            assertTrue(awaitItem())

            appSettings.setShowFollicularPhase(false)

            assertFalse(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun setShowFollicularPhase_WHEN_setToTrue_THEN_emitsTrue() = runTest {
        appSettings.setShowFollicularPhase(false)

        appSettings.showFollicularPhase.test {
            assertFalse(awaitItem())

            appSettings.setShowFollicularPhase(true)

            assertTrue(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    // --- showOvulationPhase ---

    @Test
    fun setShowOvulationPhase_WHEN_setToFalse_THEN_emitsFalse() = runTest {
        appSettings.setShowOvulationPhase(true)

        appSettings.showOvulationPhase.test {
            assertTrue(awaitItem())

            appSettings.setShowOvulationPhase(false)

            assertFalse(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun setShowOvulationPhase_WHEN_setToTrue_THEN_emitsTrue() = runTest {
        appSettings.setShowOvulationPhase(false)

        appSettings.showOvulationPhase.test {
            assertFalse(awaitItem())

            appSettings.setShowOvulationPhase(true)

            assertTrue(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    // --- showLutealPhase ---

    @Test
    fun setShowLutealPhase_WHEN_setToFalse_THEN_emitsFalse() = runTest {
        appSettings.setShowLutealPhase(true)

        appSettings.showLutealPhase.test {
            assertTrue(awaitItem())

            appSettings.setShowLutealPhase(false)

            assertFalse(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun setShowLutealPhase_WHEN_setToTrue_THEN_emitsTrue() = runTest {
        appSettings.setShowLutealPhase(false)

        appSettings.showLutealPhase.test {
            assertFalse(awaitItem())

            appSettings.setShowLutealPhase(true)

            assertTrue(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    // --- menstruationColor ---

    @Test
    fun setMenstruationColor_WHEN_setToCustomHex_THEN_emitsCustomHex() = runTest {
        appSettings.setMenstruationColor("EF9A9A")

        appSettings.menstruationColor.test {
            assertEquals("EF9A9A", awaitItem())

            appSettings.setMenstruationColor("FF0000")

            assertEquals("FF0000", awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun menstruationColor_WHEN_notSet_THEN_emitsDefault() = runTest {
        // Reset to default to verify initial emission
        appSettings.setMenstruationColor("EF9A9A")

        appSettings.menstruationColor.test {
            assertEquals("EF9A9A", awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    // --- follicularColor ---

    @Test
    fun setFollicularColor_WHEN_setToCustomHex_THEN_emitsCustomHex() = runTest {
        appSettings.setFollicularColor("80CBC4")

        appSettings.follicularColor.test {
            assertEquals("80CBC4", awaitItem())

            appSettings.setFollicularColor("00FF00")

            assertEquals("00FF00", awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun follicularColor_WHEN_notSet_THEN_emitsDefault() = runTest {
        appSettings.setFollicularColor("80CBC4")

        appSettings.follicularColor.test {
            assertEquals("80CBC4", awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    // --- ovulationColor ---

    @Test
    fun setOvulationColor_WHEN_setToCustomHex_THEN_emitsCustomHex() = runTest {
        appSettings.setOvulationColor("FFCC80")

        appSettings.ovulationColor.test {
            assertEquals("FFCC80", awaitItem())

            appSettings.setOvulationColor("AABBCC")

            assertEquals("AABBCC", awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun ovulationColor_WHEN_notSet_THEN_emitsDefault() = runTest {
        appSettings.setOvulationColor("FFCC80")

        appSettings.ovulationColor.test {
            assertEquals("FFCC80", awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    // --- lutealColor ---

    @Test
    fun setLutealColor_WHEN_setToCustomHex_THEN_emitsCustomHex() = runTest {
        appSettings.setLutealColor("B39DDB")

        appSettings.lutealColor.test {
            assertEquals("B39DDB", awaitItem())

            appSettings.setLutealColor("112233")

            assertEquals("112233", awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun lutealColor_WHEN_notSet_THEN_emitsDefault() = runTest {
        appSettings.setLutealColor("B39DDB")

        appSettings.lutealColor.test {
            assertEquals("B39DDB", awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }
}
