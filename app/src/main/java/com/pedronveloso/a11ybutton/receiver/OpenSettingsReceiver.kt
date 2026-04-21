/*
 * Copyright (C) 2026 Pedro Veloso
 * SPDX-License-Identifier: Apache-2.0
 */
package com.pedronveloso.a11ybutton.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Settings
import com.pedronveloso.a11ybutton.notifications.ServiceStatusNotifier

class OpenSettingsReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent) {
    ServiceStatusNotifier.cancelNotification(context)
    context.startActivity(
        Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
    )
  }
}
