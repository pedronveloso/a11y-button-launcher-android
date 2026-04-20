/*
 * Copyright (C) 2026 Pedro Veloso
 * SPDX-License-Identifier: Apache-2.0
 */
package com.pedronveloso.a11ybutton.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.pedronveloso.a11ybutton.R
import com.pedronveloso.a11ybutton.receiver.OptOutReceiver
import timber.log.Timber

/**
 * Manages the service-health notification that alerts the user when the accessibility service has
 * been disabled.
 *
 * Android automatically disables accessibility services whenever the app is updated, and may also
 * disable them under other system conditions. Because the user might not open the app for days,
 * [ServiceCheckWorker] runs periodically in the background and calls this object to post or clear
 * the notification as needed.
 *
 * The notification offers two actions:
 * - **Open Settings** — deep-links directly to the system Accessibility settings page.
 * - **Don't remind me** — permanently opts the user out via [OptOutReceiver], which writes the flag
 *   to DataStore so the worker becomes a no-op on future runs.
 *
 * The notification is also cancelled whenever the app comes to the foreground (see `MainRoute` in
 * `MainActivity`), so a stale alert is never shown to a user who has already re-enabled the
 * service.
 */
object ServiceStatusNotifier {
  const val CHANNEL_ID = "service_status"
  const val NOTIFICATION_ID = 1001

  fun createChannel(context: Context) {
    val name = context.getString(R.string.notification_channel_name)
    val description = context.getString(R.string.notification_channel_description)
    val channel =
        NotificationChannel(CHANNEL_ID, name, NotificationManager.IMPORTANCE_DEFAULT).apply {
          this.description = description
        }
    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    manager.createNotificationChannel(channel)
    Timber.d("Notification channel created: %s", CHANNEL_ID)
  }

  fun showServiceDisabledNotification(context: Context) {
    val openSettingsIntent =
        PendingIntent.getActivity(
            context,
            0,
            Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    val optOutIntent =
        PendingIntent.getBroadcast(
            context,
            0,
            Intent(context, OptOutReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    val notification =
        NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.notification_service_disabled_title))
            .setContentText(context.getString(R.string.notification_service_disabled_body))
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(context.getString(R.string.notification_service_disabled_body)),
            )
            .setContentIntent(openSettingsIntent)
            .setAutoCancel(true)
            .addAction(
                0,
                context.getString(R.string.notification_action_open_settings),
                openSettingsIntent,
            )
            .addAction(
                0,
                context.getString(R.string.notification_action_opt_out),
                optOutIntent,
            )
            .build()

    NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    Timber.i("Service-disabled notification posted")
  }

  fun cancelNotification(context: Context) {
    NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
  }
}
