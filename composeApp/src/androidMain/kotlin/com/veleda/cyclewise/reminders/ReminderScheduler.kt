package com.veleda.cyclewise.reminders

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.veleda.cyclewise.reminders.workers.HydrationReminderWorker
import com.veleda.cyclewise.reminders.workers.MedicationReminderWorker
import com.veleda.cyclewise.reminders.workers.PeriodPredictionWorker
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * Coordinator that enqueues and cancels WorkManager periodic jobs for all
 * three reminder types.
 *
 * Registered as a Koin singleton. Uses [ExistingPeriodicWorkPolicy.UPDATE]
 * so parameter changes (e.g. new medication time) replace the existing work
 * without cancelling first.
 *
 * @param context application context used to obtain the [WorkManager] instance.
 */
class ReminderScheduler(private val context: Context) {

    companion object {
        /** Unique work name for the period prediction reminder. */
        const val WORK_NAME_PERIOD = "reminder_period_prediction"

        /** Unique work name for the daily medication reminder. */
        const val WORK_NAME_MEDICATION = "reminder_medication_daily"

        /** Unique work name for the hydration reminder. */
        const val WORK_NAME_HYDRATION = "reminder_hydration"
    }

    private val workManager get() = WorkManager.getInstance(context)

    /**
     * Schedules or cancels the daily period prediction check.
     *
     * When enabled, enqueues a ~24h periodic worker with 1h flex that reads
     * the cached predicted date from DataStore and notifies if within window.
     *
     * @param enabled `true` to schedule, `false` to cancel.
     */
    fun schedulePeriodPrediction(enabled: Boolean) {
        if (!enabled) {
            workManager.cancelUniqueWork(WORK_NAME_PERIOD)
            return
        }
        val request = PeriodicWorkRequestBuilder<PeriodPredictionWorker>(
            24, TimeUnit.HOURS,
            1, TimeUnit.HOURS
        ).build()
        workManager.enqueueUniquePeriodicWork(
            WORK_NAME_PERIOD,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    /**
     * Schedules or cancels the daily medication reminder.
     *
     * When enabled, computes an initial delay so the first execution targets
     * the user's chosen time, then repeats every ~24h with 30min flex.
     *
     * @param enabled `true` to schedule, `false` to cancel.
     * @param hour    target hour of day (0–23).
     * @param minute  target minute of hour (0–59).
     */
    fun scheduleMedication(enabled: Boolean, hour: Int = 9, minute: Int = 0) {
        if (!enabled) {
            workManager.cancelUniqueWork(WORK_NAME_MEDICATION)
            return
        }
        val initialDelay = computeInitialDelay(hour, minute)
        val request = PeriodicWorkRequestBuilder<MedicationReminderWorker>(
            24, TimeUnit.HOURS,
            30, TimeUnit.MINUTES
        ).setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .build()
        workManager.enqueueUniquePeriodicWork(
            WORK_NAME_MEDICATION,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    /**
     * Schedules or cancels the periodic hydration reminder.
     *
     * When enabled, uses the user's chosen frequency (2/3/4 hours) as the
     * repeat interval with 15min flex.
     *
     * @param enabled        `true` to schedule, `false` to cancel.
     * @param frequencyHours interval between reminders (2, 3, or 4).
     */
    fun scheduleHydration(enabled: Boolean, frequencyHours: Int = 3) {
        if (!enabled) {
            workManager.cancelUniqueWork(WORK_NAME_HYDRATION)
            return
        }
        val request = PeriodicWorkRequestBuilder<HydrationReminderWorker>(
            frequencyHours.toLong(), TimeUnit.HOURS,
            15, TimeUnit.MINUTES
        ).build()
        workManager.enqueueUniquePeriodicWork(
            WORK_NAME_HYDRATION,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    /**
     * Cancels all three reminder work requests.
     */
    fun cancelAll() {
        workManager.cancelUniqueWork(WORK_NAME_PERIOD)
        workManager.cancelUniqueWork(WORK_NAME_MEDICATION)
        workManager.cancelUniqueWork(WORK_NAME_HYDRATION)
    }

    /**
     * Computes the delay in milliseconds from now until the next occurrence of
     * the given [hour]:[minute]. If the target time has already passed today,
     * returns the delay until that time tomorrow.
     */
    private fun computeInitialDelay(hour: Int, minute: Int): Long {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (target.before(now)) {
            target.add(Calendar.DAY_OF_MONTH, 1)
        }
        return target.timeInMillis - now.timeInMillis
    }
}
