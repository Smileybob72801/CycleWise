package com.veleda.cyclewise.reminders.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.veleda.cyclewise.androidData.local.draft.LockedWaterDraft
import com.veleda.cyclewise.reminders.ReminderNotifier
import com.veleda.cyclewise.settings.AppSettings
import kotlinx.coroutines.flow.first
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.time.Clock

/**
 * Periodic background worker that reminds the user to log water intake.
 *
 * Reads the active window (start/end hour), daily goal, and today's draft
 * water cups from [AppSettings] and [LockedWaterDraft] (both plaintext
 * DataStore). Skips notification if:
 * - The current hour is outside the `[startHour, endHour)` active window.
 * - Today's logged cups already meet or exceed the goal.
 *
 * The worker never accesses the encrypted database.
 *
 * Scheduled at the user's chosen frequency (2/3/4h periodic, 15min flex)
 * by [ReminderScheduler][com.veleda.cyclewise.reminders.ReminderScheduler].
 */
class HydrationReminderWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val appSettings = AppSettings(applicationContext)
        val lockedWaterDraft = LockedWaterDraft(applicationContext)

        val startHour = appSettings.reminderHydrationStartHour.first()
        val endHour = appSettings.reminderHydrationEndHour.first()
        val goalCups = appSettings.reminderHydrationGoalCups.first()

        val currentHour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        if (currentHour < startHour || currentHour >= endHour) {
            return Result.success()
        }

        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val drafts = lockedWaterDraft.drafts.first()
        val todayCups = drafts[today] ?: 0

        if (todayCups >= goalCups) {
            return Result.success()
        }

        ReminderNotifier.notify(applicationContext, ReminderNotifier.NOTIFICATION_ID_HYDRATION)
        return Result.success()
    }
}
