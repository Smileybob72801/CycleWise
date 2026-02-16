package com.veleda.cyclewise.reminders

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.veleda.cyclewise.MainActivity
import com.veleda.cyclewise.R

/**
 * Singleton responsible for creating the notification channel and posting
 * privacy-safe reminder notifications.
 *
 * All notification content is **static** — title and body are string resources
 * containing no health data. This is enforced here so individual workers cannot
 * accidentally leak sensitive information.
 */
object ReminderNotifier {

    private const val CHANNEL_ID = "reminders"
    private const val CHANNEL_NAME = "Reminders"

    /** Notification ID for the period prediction reminder. */
    const val NOTIFICATION_ID_PERIOD = 1001

    /** Notification ID for the daily medication reminder. */
    const val NOTIFICATION_ID_MEDICATION = 1002

    /** Notification ID for the hydration reminder. */
    const val NOTIFICATION_ID_HYDRATION = 1003

    /**
     * Creates the `"reminders"` notification channel.
     *
     * Safe to call multiple times — [NotificationManager.createNotificationChannel]
     * is idempotent. Must be called before any worker attempts to post a notification.
     *
     * @param context application context.
     */
    fun ensureChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_DEFAULT
        )
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    /**
     * Posts a notification with static, privacy-safe content.
     *
     * Title and body come from string resources (`reminder_notification_title` /
     * `reminder_notification_body`). Tapping the notification opens [MainActivity].
     * Silently no-ops if the POST_NOTIFICATIONS permission is not granted.
     *
     * @param context        application context.
     * @param notificationId one of [NOTIFICATION_ID_PERIOD], [NOTIFICATION_ID_MEDICATION],
     *                       or [NOTIFICATION_ID_HYDRATION].
     */
    fun notify(context: Context, notificationId: Int) {
        if (!hasPermission(context)) return

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(context.getString(R.string.reminder_notification_title))
            .setContentText(context.getString(R.string.reminder_notification_body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(notificationId, notification)
    }

    /**
     * Returns `true` if the app has permission to post notifications.
     *
     * On API < 33 (pre-Tiramisu), POST_NOTIFICATIONS is not required, so this
     * always returns `true`. On API 33+, checks the runtime permission.
     *
     * @param context application context.
     */
    fun hasPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }
}
