package com.veleda.cyclewise.reminders

import android.Manifest
import android.app.Application
import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowNotificationManager
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Unit tests for [ReminderNotifier] verifying notification channel creation,
 * notification posting, and privacy-safe content.
 *
 * Tests that post notifications run on API 32 (pre-Tiramisu) to avoid
 * the POST_NOTIFICATIONS runtime permission gate.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = com.veleda.cyclewise.RobolectricTestApp::class)
class ReminderNotifierTest {

    private lateinit var context: Context
    private lateinit var notificationManager: NotificationManager
    private lateinit var shadowNotificationManager: ShadowNotificationManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        notificationManager = context.getSystemService(NotificationManager::class.java)
        shadowNotificationManager = Shadows.shadowOf(notificationManager)
    }

    @Test
    fun ensureChannel_WHEN_called_THEN_channelCreated() {
        // GIVEN no channel exists yet
        // WHEN
        ReminderNotifier.ensureChannel(context)

        // THEN
        val channel = notificationManager.getNotificationChannel("reminders")
        assertNotNull(channel, "Notification channel 'reminders' should exist")
        assertEquals("Reminders", channel.name.toString())
    }

    @Test
    @Config(sdk = [32])
    fun notify_WHEN_permissionGranted_THEN_notificationPosted() {
        // GIVEN channel exists and permission is granted (API 32 < Tiramisu, no runtime check)
        ReminderNotifier.ensureChannel(context)

        // WHEN
        ReminderNotifier.notify(context, ReminderNotifier.NOTIFICATION_ID_PERIOD)

        // THEN — verify notification was posted via shadow
        assertTrue(
            shadowNotificationManager.size() > 0,
            "At least one notification should be posted"
        )
        val notification = shadowNotificationManager.getNotification(ReminderNotifier.NOTIFICATION_ID_PERIOD)
        assertNotNull(notification, "Notification with NOTIFICATION_ID_PERIOD should be posted")
    }

    @Test
    @Config(sdk = [32])
    fun notify_WHEN_contentChecked_THEN_containsNoHealthData() {
        // GIVEN (API 32, no runtime permission needed)
        ReminderNotifier.ensureChannel(context)

        // WHEN
        ReminderNotifier.notify(context, ReminderNotifier.NOTIFICATION_ID_MEDICATION)

        // THEN — verify static content contains no health-related keywords
        val notification = shadowNotificationManager.getNotification(ReminderNotifier.NOTIFICATION_ID_MEDICATION)
        assertNotNull(notification, "Notification should be posted")

        val title = notification.extras.getString("android.title") ?: ""
        val body = notification.extras.getString("android.text") ?: ""
        val combined = "$title $body".lowercase()

        val bannedKeywords = listOf("period", "ovulation", "medication", "bleeding", "menstrual", "cycle")
        bannedKeywords.forEach { keyword ->
            assertTrue(
                keyword !in combined,
                "Notification content must not contain health keyword '$keyword' — found in: '$combined'"
            )
        }
    }
}
