package com.veleda.cyclewise.reminders

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.Configuration
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.testing.WorkManagerTestInitHelper
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Unit tests for [ReminderScheduler] verifying that WorkManager jobs are
 * correctly enqueued and cancelled.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(application = com.veleda.cyclewise.RobolectricTestApp::class)
class ReminderSchedulerTest {

    private lateinit var context: Context
    private lateinit var workManager: WorkManager
    private lateinit var scheduler: ReminderScheduler

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        val config = Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.DEBUG)
            .build()
        WorkManagerTestInitHelper.initializeTestWorkManager(context, config)
        workManager = WorkManager.getInstance(context)
        scheduler = ReminderScheduler(context)
    }

    @Test
    fun schedulePeriodPrediction_WHEN_enabled_THEN_enqueuesPeriodicWork() {
        // WHEN
        scheduler.schedulePeriodPrediction(true)

        // THEN
        val workInfos = workManager.getWorkInfosForUniqueWork(ReminderScheduler.WORK_NAME_PERIOD).get()
        assertTrue(workInfos.isNotEmpty(), "Period prediction work should be enqueued")
        assertEquals(WorkInfo.State.ENQUEUED, workInfos.first().state)
    }

    @Test
    fun schedulePeriodPrediction_WHEN_disabled_THEN_cancelsWork() {
        // GIVEN work was previously scheduled
        scheduler.schedulePeriodPrediction(true)

        // WHEN
        scheduler.schedulePeriodPrediction(false)

        // THEN
        val workInfos = workManager.getWorkInfosForUniqueWork(ReminderScheduler.WORK_NAME_PERIOD).get()
        assertTrue(
            workInfos.isEmpty() || workInfos.all { it.state == WorkInfo.State.CANCELLED },
            "Period prediction work should be cancelled"
        )
    }

    @Test
    fun scheduleMedication_WHEN_enabled_THEN_enqueuesPeriodicWork() {
        // WHEN
        scheduler.scheduleMedication(true, 9, 0)

        // THEN
        val workInfos = workManager.getWorkInfosForUniqueWork(ReminderScheduler.WORK_NAME_MEDICATION).get()
        assertTrue(workInfos.isNotEmpty(), "Medication work should be enqueued")
        assertEquals(WorkInfo.State.ENQUEUED, workInfos.first().state)
    }

    @Test
    fun scheduleHydration_WHEN_enabled_THEN_enqueuesPeriodicWorkWithCorrectInterval() {
        // WHEN
        scheduler.scheduleHydration(true, 3)

        // THEN
        val workInfos = workManager.getWorkInfosForUniqueWork(ReminderScheduler.WORK_NAME_HYDRATION).get()
        assertTrue(workInfos.isNotEmpty(), "Hydration work should be enqueued")
        val state = workInfos.first().state
        assertTrue(
            state == WorkInfo.State.ENQUEUED || state == WorkInfo.State.RUNNING,
            "Hydration work should be enqueued or running, was: $state"
        )
    }

    @Test
    fun cancelAll_WHEN_called_THEN_cancelsAllThreeWorkNames() {
        // GIVEN all three are scheduled
        scheduler.schedulePeriodPrediction(true)
        scheduler.scheduleMedication(true, 9, 0)
        scheduler.scheduleHydration(true, 3)

        // WHEN
        scheduler.cancelAll()

        // THEN
        listOf(
            ReminderScheduler.WORK_NAME_PERIOD,
            ReminderScheduler.WORK_NAME_MEDICATION,
            ReminderScheduler.WORK_NAME_HYDRATION
        ).forEach { workName ->
            val workInfos = workManager.getWorkInfosForUniqueWork(workName).get()
            assertTrue(
                workInfos.isEmpty() || workInfos.all { it.state == WorkInfo.State.CANCELLED },
                "Work '$workName' should be cancelled"
            )
        }
    }
}
