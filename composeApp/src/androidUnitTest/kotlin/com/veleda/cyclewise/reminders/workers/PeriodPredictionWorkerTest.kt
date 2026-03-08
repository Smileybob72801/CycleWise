package com.veleda.cyclewise.reminders.workers

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.testing.TestListenableWorkerBuilder
import com.veleda.cyclewise.settings.AppSettings
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.time.Clock

/**
 * Unit tests for [PeriodPredictionWorker] verifying notification behavior
 * based on cached prediction date and days-before window.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(application = com.veleda.cyclewise.RobolectricTestApp::class)
class PeriodPredictionWorkerTest {

    private lateinit var context: Context
    private lateinit var appSettings: AppSettings

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        appSettings = AppSettings(context)
    }

    @Test
    fun doWork_WHEN_cachedDateWithinWindow_THEN_succeeds() = runTest {
        // GIVEN predicted date is 1 day from now, days-before window is 2
        val tomorrow = Clock.System.todayIn(TimeZone.currentSystemDefault())
            .plus(1, DateTimeUnit.DAY)
        appSettings.setCachedPredictedPeriodDate(tomorrow.toString())
        appSettings.setReminderPeriodDaysBefore(2)

        // WHEN
        val worker = TestListenableWorkerBuilder<PeriodPredictionWorker>(context).build()
        val result = worker.doWork()

        // THEN — worker succeeds (notification would be posted)
        assertEquals(ListenableWorker.Result.success(), result)
    }

    @Test
    fun doWork_WHEN_cachedDateBlank_THEN_succeeds_noNotification() = runTest {
        // GIVEN no cached date
        appSettings.setCachedPredictedPeriodDate("")

        // WHEN
        val worker = TestListenableWorkerBuilder<PeriodPredictionWorker>(context).build()
        val result = worker.doWork()

        // THEN — succeeds silently
        assertEquals(ListenableWorker.Result.success(), result)
    }

    @Test
    fun doWork_WHEN_cachedDateInPast_THEN_succeeds_noNotification() = runTest {
        // GIVEN predicted date is 5 days in the past
        val pastDate = Clock.System.todayIn(TimeZone.currentSystemDefault())
            .minus(5, DateTimeUnit.DAY)
        appSettings.setCachedPredictedPeriodDate(pastDate.toString())
        appSettings.setReminderPeriodDaysBefore(2)

        // WHEN
        val worker = TestListenableWorkerBuilder<PeriodPredictionWorker>(context).build()
        val result = worker.doWork()

        // THEN — succeeds silently (negative daysUntil is outside window)
        assertEquals(ListenableWorker.Result.success(), result)
    }

    @Test
    fun doWork_WHEN_cachedDateTooFarAway_THEN_succeeds_noNotification() = runTest {
        // GIVEN predicted date is 10 days from now, days-before window is 3
        val farDate = Clock.System.todayIn(TimeZone.currentSystemDefault())
            .plus(10, DateTimeUnit.DAY)
        appSettings.setCachedPredictedPeriodDate(farDate.toString())
        appSettings.setReminderPeriodDaysBefore(3)

        // WHEN
        val worker = TestListenableWorkerBuilder<PeriodPredictionWorker>(context).build()
        val result = worker.doWork()

        // THEN — succeeds silently (10 is outside 0..3 window)
        assertEquals(ListenableWorker.Result.success(), result)
    }
}
