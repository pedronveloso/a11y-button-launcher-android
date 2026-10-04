/*
 * Copyright (C) 2026 Pedro Veloso
 * SPDX-License-Identifier: Apache-2.0
 */
package com.pedronveloso.a11ybutton.service

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
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

  /**
   * Rebuilds the intent saved for a shortcut. The URI was produced by another app, so it is treated
   * as untrusted: the selector is dropped, URI grant flags are stripped, and the intent must target
   * the shortcut's own package (implicit intents are pinned to it).
   */
  fun createShortcutIntent(
      intentUri: String,
      expectedPackage: String,
  ): Intent? {
    val intent =
        try {
          Intent.parseUri(intentUri, Intent.URI_INTENT_SCHEME)
        } catch (exception: java.net.URISyntaxException) {
          Timber.w(exception, "Cannot parse saved shortcut intent")
          return null
        }
    val targetPackage = intent.component?.packageName ?: intent.`package`
    if (targetPackage == null) {
      intent.setPackage(expectedPackage)
    } else if (targetPackage != expectedPackage) {
      Timber.w(
          "Shortcut intent targets package=%s but shortcut belongs to %s",
          targetPackage,
          expectedPackage,
      )
      return null
    }
    intent.selector = null
    intent.sourceBounds = null
    intent.flags = (intent.flags and GRANT_URI_FLAGS.inv()) or SHORTCUT_FLAGS
    return intent
  }

  private const val GRANT_URI_FLAGS =
      Intent.FLAG_GRANT_READ_URI_PERMISSION or
          Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
          Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION or
          Intent.FLAG_GRANT_PREFIX_URI_PERMISSION

  private const val SHORTCUT_FLAGS = Intent.FLAG_ACTIVITY_NEW_TASK

  /**
   * Whether this app is allowed to start [intent]. Explicit intents resolve even when the target is
   * not exported, so exported and permission are checked here: launchers can start such shortcut
   * targets through a privileged API, but a normal app would get a [SecurityException].
   */
  fun isLaunchable(
      packageManager: PackageManager,
      intent: Intent,
  ): Boolean {
    val activityInfo = packageManager.resolveActivity(intent, 0)?.activityInfo ?: return false
    return activityInfo.exported && activityInfo.permission == null
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
