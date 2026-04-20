/*
 * Copyright (C) 2026 Pedro Veloso
 * SPDX-License-Identifier: Apache-2.0
 */
package com.pedronveloso.a11ybutton.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.pedronveloso.a11ybutton.data.SettingsRepository
import com.pedronveloso.a11ybutton.notifications.ServiceStatusNotifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

class OptOutReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent) {
    val pendingResult = goAsync()
    CoroutineScope(Dispatchers.IO).launch {
      try {
        SettingsRepository.fromContext(context).setNotificationsOptedOut(true)
        ServiceStatusNotifier.cancelNotification(context)
        Timber.i("User opted out of service-health notifications")
      } finally {
        pendingResult.finish()
      }
    }
  }
}
