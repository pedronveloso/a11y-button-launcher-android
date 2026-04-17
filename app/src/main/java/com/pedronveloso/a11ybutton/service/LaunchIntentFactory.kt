/*
 * Copyright (C) 2026 Pedro Veloso
 * All rights reserved.
 */
package com.pedronveloso.a11ybutton.service

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import com.pedronveloso.a11ybutton.MainActivity
import timber.log.Timber

object LaunchIntentFactory {
  private const val HOST_APP_FLAGS =
      Intent.FLAG_ACTIVITY_NEW_TASK or
          Intent.FLAG_ACTIVITY_CLEAR_TOP or
          Intent.FLAG_ACTIVITY_SINGLE_TOP

  private const val TARGET_APP_FLAGS =
      Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED

  fun createTargetAppIntent(componentName: String): Intent? {
    val component =
        ComponentName.unflattenFromString(componentName)
            ?: run {
              Timber.w("Cannot create launch intent for malformed component=%s", componentName)
              return null
            }
    return Intent(Intent.ACTION_MAIN)
        .addCategory(Intent.CATEGORY_LAUNCHER)
        .setComponent(component)
        .addFlags(TARGET_APP_FLAGS)
        .also { Timber.d("Created target app launch intent for component=%s", componentName) }
  }

  fun createHostAppIntent(
      context: Context,
      message: String,
  ): Intent =
      Intent(context, MainActivity::class.java)
          .addFlags(HOST_APP_FLAGS)
          .putExtra(MainActivity.EXTRA_STATUS_MESSAGE, message)
          .also { Timber.i("Opening host app with message=%s", message) }
}
