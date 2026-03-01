package com.veleda.cyclewise.domain.usecases

import android.content.Context
import com.veleda.cyclewise.androidData.local.draft.LockedWaterDraft
import com.veleda.cyclewise.reminders.ReminderScheduler
import com.veleda.cyclewise.services.SaltStorage
import com.veleda.cyclewise.settings.AppSettings
import com.veleda.cyclewise.ui.coachmark.HintPreferences

/**
 * Singleton-scoped use case that irreversibly deletes **all** user data, returning
 * the app to a fresh-install state.
 *
 * The caller is responsible for closing the Koin session scope **before** invoking
 * this use case so that the encrypted database file is no longer held open.
 *
 * Deletion steps (order matters only for the database file lock):
 * 1. Delete the encrypted SQLCipher database file.
 * 2. Clear all DataStore app preferences.
 * 3. Clear the encryption salt from SharedPreferences.
 * 4. Reset all coach-mark / tutorial hint flags.
 * 5. Clear the locked water draft DataStore.
 * 6. Cancel all scheduled WorkManager reminders.
 *
 * @param context           application context used to delete the database file.
 * @param appSettings       DataStore-backed app preferences.
 * @param saltStorage       SharedPreferences-backed encryption salt store.
 * @param hintPreferences   DataStore-backed coach-mark hint flags.
 * @param lockedWaterDraft  DataStore-backed lock-screen water draft store.
 * @param reminderScheduler WorkManager reminder coordinator.
 */
class DeleteAllDataUseCase(
    private val context: Context,
    private val appSettings: AppSettings,
    private val saltStorage: SaltStorage,
    private val hintPreferences: HintPreferences,
    private val lockedWaterDraft: LockedWaterDraft,
    private val reminderScheduler: ReminderScheduler,
) {

    /**
     * Executes the full data wipe.
     *
     * Must be called from a coroutine context (suspending for DataStore operations).
     * The session scope **must** already be closed before calling this method.
     */
    suspend operator fun invoke() {
        context.deleteDatabase("cyclewise.db")
        appSettings.clearAll()
        saltStorage.clear()
        hintPreferences.resetAll()
        lockedWaterDraft.clearAll()
        reminderScheduler.cancelAll()
    }
}
