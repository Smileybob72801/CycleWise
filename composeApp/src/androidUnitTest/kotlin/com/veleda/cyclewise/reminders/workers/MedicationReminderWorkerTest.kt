package com.veleda.cyclewise.reminders.workers

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.testing.TestListenableWorkerBuilder
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals

/**
 * Unit tests for [MedicationReminderWorker].
 *
 * The worker simply posts a notification on every execution — scheduling
 * (including time-of-day targeting) is handled by [ReminderScheduler].
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(application = com.veleda.cyclewise.RobolectricTestApp::class)
class MedicationReminderWorkerTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun doWork_WHEN_called_THEN_succeeds() = runTest {
        // GIVEN a medication reminder worker
        val worker = TestListenableWorkerBuilder<MedicationReminderWorker>(context).build()

        // WHEN
        val result = worker.doWork()

        // THEN — always succeeds
        assertEquals(ListenableWorker.Result.success(), result)
    }
}
