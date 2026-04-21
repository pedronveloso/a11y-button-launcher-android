/*
 * Copyright (C) 2026 Pedro Veloso
 * SPDX-License-Identifier: Apache-2.0
 */
package com.pedronveloso.a11ybutton.work

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.pedronveloso.a11ybutton.data.AccessibilityStatusRepository
import com.pedronveloso.a11ybutton.data.SettingsRepository
import com.pedronveloso.a11ybutton.notifications.ServiceStatusNotifier
import com.pedronveloso.a11ybutton.service.ShortcutLaunchAccessibilityService
import kotlinx.coroutines.flow.first
import timber.log.Timber

class ServiceCheckWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {
  companion object {
    const val UNIQUE_WORK_NAME = "service_check"
  }

  override suspend fun doWork(): Result {
    val settings = SettingsRepository.fromContext(applicationContext).settings.first()

    if (!settings.notificationsEnabled) {
      Timber.d("Service check skipped: notifications not enabled by user")
      return Result.success()
    }

    if (settings.notificationsOptedOut) {
      Timber.d("Service check skipped: user opted out of notifications")
      return Result.success()
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      val permissionState =
          ContextCompat.checkSelfPermission(
              applicationContext,
              Manifest.permission.POST_NOTIFICATIONS,
          )
      if (permissionState != PackageManager.PERMISSION_GRANTED) {
        Timber.d("Service check skipped: POST_NOTIFICATIONS permission not granted")
        return Result.success()
      }
    }

    val serviceComponent =
        ComponentName(applicationContext, ShortcutLaunchAccessibilityService::class.java)
    val isEnabled =
        AccessibilityStatusRepository.isServiceEnabled(applicationContext, serviceComponent)

    return if (isEnabled) {
      Timber.d("Service check: accessibility service is enabled, no notification needed")
      ServiceStatusNotifier.cancelNotification(applicationContext)
      Result.success()
    } else {
      Timber.i("Service check: accessibility service is disabled, posting notification")
      ServiceStatusNotifier.showServiceDisabledNotification(applicationContext)
      Result.success()
    }
  }
}
