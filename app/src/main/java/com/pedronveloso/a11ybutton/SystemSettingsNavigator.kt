/*
 * Copyright (C) 2026 Pedro Veloso
 * SPDX-License-Identifier: Apache-2.0
 */
package com.pedronveloso.a11ybutton

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import timber.log.Timber

object SystemSettingsNavigator {
  fun isIgnoringBatteryOptimizations(context: Context): Boolean {
    val powerManager = context.getSystemService(PowerManager::class.java)
    return powerManager?.isIgnoringBatteryOptimizations(context.packageName) == true
  }

  fun openAccessibilitySettings(context: Context) {
    launchActivity(
        context = context,
        intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS),
        description = "accessibility settings",
    )
  }

  fun openBatteryOptimizationSettings(context: Context) {
    launchActivity(
        context = context,
        intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS),
        description = "battery optimization settings",
    )
  }

  @SuppressLint("BatteryLife")
  fun requestIgnoreBatteryOptimizations(context: Context) {
    val packageUri = Uri.fromParts("package", context.packageName, null)
    launchActivity(
        context = context,
        intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).setData(packageUri),
        description = "battery optimization exemption prompt",
        fallback = { openBatteryOptimizationSettings(context) },
    )
  }

  fun openAppDetails(context: Context) {
    launchActivity(
        context = context,
        intent =
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                .setData(Uri.fromParts("package", context.packageName, null)),
        description = "app details",
    )
  }

  fun openApp(context: Context) {
    launchActivity(
        context = context,
        intent = Intent(context, MainActivity::class.java),
        description = "app",
    )
  }

  private fun launchActivity(
      context: Context,
      intent: Intent,
      description: String,
      fallback: (() -> Unit)? = null,
  ) {
    val safeIntent = intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
      Timber.i("Opening %s", description)
      context.startActivity(safeIntent)
    } catch (exception: ActivityNotFoundException) {
      Timber.e(exception, "Unable to open %s", description)
      fallback?.invoke()
    }
  }
}
