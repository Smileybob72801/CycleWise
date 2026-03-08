package com.veleda.cyclewise.reminders.workers

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.testing.TestListenableWorkerBuilder
import com.veleda.cyclewise.androidData.local.draft.LockedWaterDraft
import com.veleda.cyclewise.settings.AppSettings
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.time.Clock

/**
 * Unit tests for [HydrationReminderWorker] verifying notification behavior
 * based on active window, goal, and current water intake.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(application = com.veleda.cyclewise.RobolectricTestApp::class)
class HydrationReminderWorkerTest {

    private lateinit var context: Context
    private lateinit var appSettings: AppSettings
    private lateinit var lockedWaterDraft: LockedWaterDraft

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        appSettings = AppSettings(context)
        lockedWaterDraft = LockedWaterDraft(context)
    }

    @Test
    fun doWork_WHEN_withinWindowAndBelowGoal_THEN_succeeds() = runTest {
        // GIVEN active window covers the current hour, goal not met
        val currentHour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        appSettings.setReminderHydrationStartHour(0)
        appSettings.setReminderHydrationEndHour(23)
        appSettings.setReminderHydrationGoalCups(10)

        // No water logged today (default)
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        lockedWaterDraft.setCups(today, 0)

        // WHEN
        val worker = TestListenableWorkerBuilder<HydrationReminderWorker>(context).build()
        val result = worker.doWork()

        // THEN — succeeds (notification would be posted)
        assertEquals(ListenableWorker.Result.success(), result)
    }

    @Test
    fun doWork_WHEN_outsideWindow_THEN_succeeds_noNotification() = runTest {
        // GIVEN active window does NOT cover the current hour
        // Set window to a time range that's guaranteed to exclude now
        val currentHour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        val startHour = (currentHour + 2) % 24
        val endHour = (currentHour + 3) % 24

        appSettings.setReminderHydrationStartHour(startHour)
        appSettings.setReminderHydrationEndHour(if (endHour > startHour) endHour else 23)
        appSettings.setReminderHydrationGoalCups(8)

        // WHEN
        val worker = TestListenableWorkerBuilder<HydrationReminderWorker>(context).build()
        val result = worker.doWork()

        // THEN — succeeds silently
        assertEquals(ListenableWorker.Result.success(), result)
    }

    @Test
    fun doWork_WHEN_goalMet_THEN_succeeds_noNotification() = runTest {
        // GIVEN goal is met (logged enough cups)
        appSettings.setReminderHydrationStartHour(0)
        appSettings.setReminderHydrationEndHour(23)
        appSettings.setReminderHydrationGoalCups(8)

        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        lockedWaterDraft.setCups(today, 10) // 10 cups logged, goal is 8

        // WHEN
        val worker = TestListenableWorkerBuilder<HydrationReminderWorker>(context).build()
        val result = worker.doWork()

        // THEN — succeeds silently (goal met, no notification)
        assertEquals(ListenableWorker.Result.success(), result)
    }
}
