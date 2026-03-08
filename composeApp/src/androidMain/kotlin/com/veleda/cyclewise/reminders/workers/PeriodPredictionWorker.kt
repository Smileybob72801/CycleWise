package com.veleda.cyclewise.reminders.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.veleda.cyclewise.reminders.ReminderNotifier
import com.veleda.cyclewise.settings.AppSettings
import kotlinx.coroutines.flow.first
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.todayIn
import kotlin.time.Clock

/**
 * Daily background worker that checks whether the predicted next period date
 * falls within the user's configured notification window.
 *
 * Reads the cached predicted date and "days before" setting from [AppSettings]
 * (plaintext DataStore). If the prediction is blank, unparseable, or outside
 * the window, the worker completes silently. The worker never accesses the
 * encrypted database.
 *
 * Scheduled as ~24h periodic work with 1h flex by [ReminderScheduler][com.veleda.cyclewise.reminders.ReminderScheduler].
 */
class PeriodPredictionWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val appSettings = AppSettings(applicationContext)
        val cachedDate = appSettings.cachedPredictedPeriodDate.first()
        if (cachedDate.isBlank()) return Result.success()

        val predictedDate = runCatching { LocalDate.parse(cachedDate) }.getOrNull()
            ?: return Result.success()

        val daysBefore = appSettings.reminderPeriodDaysBefore.first()
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val daysUntil = today.daysUntil(predictedDate)

        if (daysUntil in 0..daysBefore) {
            ReminderNotifier.notify(applicationContext, ReminderNotifier.NOTIFICATION_ID_PERIOD)
        }

        return Result.success()
    }
}
