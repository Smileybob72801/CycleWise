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
 * Unit tests for the reminder-related preferences in [AppSettings].
 *
 * Follows the same pattern as [AppSettingsTest]: Robolectric + Turbine,
 * Given-When-Then structure, roundtrip set/read verification.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(application = com.veleda.cyclewise.RobolectricTestApp::class)
class AppSettingsReminderTest {

    private lateinit var appSettings: AppSettings

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        appSettings = AppSettings(context)
    }

    // --- reminderPeriodEnabled ---

    @Test
    fun setReminderPeriodEnabled_WHEN_setToTrue_THEN_emitsTrue() = runTest {
        appSettings.setReminderPeriodEnabled(false)

        appSettings.reminderPeriodEnabled.test {
            assertFalse(awaitItem())

            appSettings.setReminderPeriodEnabled(true)

            assertTrue(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun setReminderPeriodEnabled_WHEN_setToFalse_THEN_emitsFalse() = runTest {
        appSettings.setReminderPeriodEnabled(true)

        appSettings.reminderPeriodEnabled.test {
            assertTrue(awaitItem())

            appSettings.setReminderPeriodEnabled(false)

            assertFalse(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    // --- reminderPeriodDaysBefore ---

    @Test
    fun setReminderPeriodDaysBefore_WHEN_setTo1_THEN_emits1() = runTest {
        appSettings.setReminderPeriodDaysBefore(2)

        appSettings.reminderPeriodDaysBefore.test {
            assertEquals(2, awaitItem())

            appSettings.setReminderPeriodDaysBefore(1)

            assertEquals(1, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun setReminderPeriodDaysBefore_WHEN_setTo3_THEN_emits3() = runTest {
        appSettings.setReminderPeriodDaysBefore(2)

        appSettings.reminderPeriodDaysBefore.test {
            assertEquals(2, awaitItem())

            appSettings.setReminderPeriodDaysBefore(3)

            assertEquals(3, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    // --- reminderPeriodPrivacyAccepted ---

    @Test
    fun setReminderPeriodPrivacyAccepted_WHEN_setToTrue_THEN_emitsTrue() = runTest {
        appSettings.setReminderPeriodPrivacyAccepted(false)

        appSettings.reminderPeriodPrivacyAccepted.test {
            assertFalse(awaitItem())

            appSettings.setReminderPeriodPrivacyAccepted(true)

            assertTrue(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun setReminderPeriodPrivacyAccepted_WHEN_setToFalse_THEN_emitsFalse() = runTest {
        appSettings.setReminderPeriodPrivacyAccepted(true)

        appSettings.reminderPeriodPrivacyAccepted.test {
            assertTrue(awaitItem())

            appSettings.setReminderPeriodPrivacyAccepted(false)

            assertFalse(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    // --- reminderMedicationEnabled ---

    @Test
    fun setReminderMedicationEnabled_WHEN_setToTrue_THEN_emitsTrue() = runTest {
        appSettings.setReminderMedicationEnabled(false)

        appSettings.reminderMedicationEnabled.test {
            assertFalse(awaitItem())

            appSettings.setReminderMedicationEnabled(true)

            assertTrue(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun setReminderMedicationEnabled_WHEN_setToFalse_THEN_emitsFalse() = runTest {
        appSettings.setReminderMedicationEnabled(true)

        appSettings.reminderMedicationEnabled.test {
            assertTrue(awaitItem())

            appSettings.setReminderMedicationEnabled(false)

            assertFalse(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    // --- reminderMedicationHour ---

    @Test
    fun setReminderMedicationHour_WHEN_setTo14_THEN_emits14() = runTest {
        appSettings.setReminderMedicationHour(9)

        appSettings.reminderMedicationHour.test {
            assertEquals(9, awaitItem())

            appSettings.setReminderMedicationHour(14)

            assertEquals(14, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun setReminderMedicationHour_WHEN_setTo0_THEN_emits0() = runTest {
        appSettings.setReminderMedicationHour(9)

        appSettings.reminderMedicationHour.test {
            assertEquals(9, awaitItem())

            appSettings.setReminderMedicationHour(0)

            assertEquals(0, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    // --- reminderMedicationMinute ---

    @Test
    fun setReminderMedicationMinute_WHEN_setTo30_THEN_emits30() = runTest {
        appSettings.setReminderMedicationMinute(0)

        appSettings.reminderMedicationMinute.test {
            assertEquals(0, awaitItem())

            appSettings.setReminderMedicationMinute(30)

            assertEquals(30, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun setReminderMedicationMinute_WHEN_setTo45_THEN_emits45() = runTest {
        appSettings.setReminderMedicationMinute(0)

        appSettings.reminderMedicationMinute.test {
            assertEquals(0, awaitItem())

            appSettings.setReminderMedicationMinute(45)

            assertEquals(45, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    // --- reminderHydrationEnabled ---

    @Test
    fun setReminderHydrationEnabled_WHEN_setToTrue_THEN_emitsTrue() = runTest {
        appSettings.setReminderHydrationEnabled(false)

        appSettings.reminderHydrationEnabled.test {
            assertFalse(awaitItem())

            appSettings.setReminderHydrationEnabled(true)

            assertTrue(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun setReminderHydrationEnabled_WHEN_setToFalse_THEN_emitsFalse() = runTest {
        appSettings.setReminderHydrationEnabled(true)

        appSettings.reminderHydrationEnabled.test {
            assertTrue(awaitItem())

            appSettings.setReminderHydrationEnabled(false)

            assertFalse(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    // --- reminderHydrationGoalCups ---

    @Test
    fun setReminderHydrationGoalCups_WHEN_setTo12_THEN_emits12() = runTest {
        appSettings.setReminderHydrationGoalCups(8)

        appSettings.reminderHydrationGoalCups.test {
            assertEquals(8, awaitItem())

            appSettings.setReminderHydrationGoalCups(12)

            assertEquals(12, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun setReminderHydrationGoalCups_WHEN_setTo4_THEN_emits4() = runTest {
        appSettings.setReminderHydrationGoalCups(8)

        appSettings.reminderHydrationGoalCups.test {
            assertEquals(8, awaitItem())

            appSettings.setReminderHydrationGoalCups(4)

            assertEquals(4, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    // --- reminderHydrationFrequencyHours ---

    @Test
    fun setReminderHydrationFrequencyHours_WHEN_setTo2_THEN_emits2() = runTest {
        appSettings.setReminderHydrationFrequencyHours(3)

        appSettings.reminderHydrationFrequencyHours.test {
            assertEquals(3, awaitItem())

            appSettings.setReminderHydrationFrequencyHours(2)

            assertEquals(2, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun setReminderHydrationFrequencyHours_WHEN_setTo4_THEN_emits4() = runTest {
        appSettings.setReminderHydrationFrequencyHours(3)

        appSettings.reminderHydrationFrequencyHours.test {
            assertEquals(3, awaitItem())

            appSettings.setReminderHydrationFrequencyHours(4)

            assertEquals(4, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    // --- reminderHydrationStartHour ---

    @Test
    fun setReminderHydrationStartHour_WHEN_setTo6_THEN_emits6() = runTest {
        appSettings.setReminderHydrationStartHour(8)

        appSettings.reminderHydrationStartHour.test {
            assertEquals(8, awaitItem())

            appSettings.setReminderHydrationStartHour(6)

            assertEquals(6, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun setReminderHydrationStartHour_WHEN_setTo10_THEN_emits10() = runTest {
        appSettings.setReminderHydrationStartHour(8)

        appSettings.reminderHydrationStartHour.test {
            assertEquals(8, awaitItem())

            appSettings.setReminderHydrationStartHour(10)

            assertEquals(10, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    // --- reminderHydrationEndHour ---

    @Test
    fun setReminderHydrationEndHour_WHEN_setTo18_THEN_emits18() = runTest {
        appSettings.setReminderHydrationEndHour(20)

        appSettings.reminderHydrationEndHour.test {
            assertEquals(20, awaitItem())

            appSettings.setReminderHydrationEndHour(18)

            assertEquals(18, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun setReminderHydrationEndHour_WHEN_setTo22_THEN_emits22() = runTest {
        appSettings.setReminderHydrationEndHour(20)

        appSettings.reminderHydrationEndHour.test {
            assertEquals(20, awaitItem())

            appSettings.setReminderHydrationEndHour(22)

            assertEquals(22, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    // --- cachedPredictedPeriodDate ---

    @Test
    fun setCachedPredictedPeriodDate_WHEN_setToIsoDate_THEN_emitsIsoDate() = runTest {
        appSettings.setCachedPredictedPeriodDate("")

        appSettings.cachedPredictedPeriodDate.test {
            assertEquals("", awaitItem())

            appSettings.setCachedPredictedPeriodDate("2026-03-15")

            assertEquals("2026-03-15", awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun setCachedPredictedPeriodDate_WHEN_clearedToEmpty_THEN_emitsEmpty() = runTest {
        appSettings.setCachedPredictedPeriodDate("2026-03-15")

        appSettings.cachedPredictedPeriodDate.test {
            assertEquals("2026-03-15", awaitItem())

            appSettings.setCachedPredictedPeriodDate("")

            assertEquals("", awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }
}
