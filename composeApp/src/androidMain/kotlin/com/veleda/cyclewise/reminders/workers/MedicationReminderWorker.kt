package com.veleda.cyclewise.reminders.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.veleda.cyclewise.reminders.ReminderNotifier

/**
 * Daily background worker that posts a generic medication reminder notification.
 *
 * Scheduling (including time-of-day targeting via initial delay) is handled by
 * [ReminderScheduler][com.veleda.cyclewise.reminders.ReminderScheduler]. The worker
 * itself simply fires the notification on every execution.
 *
 * Scheduled as ~24h periodic work with 30min flex.
 */
class MedicationReminderWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        ReminderNotifier.notify(applicationContext, ReminderNotifier.NOTIFICATION_ID_MEDICATION)
        return Result.success()
    }
}
