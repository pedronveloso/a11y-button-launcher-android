/*
 * Copyright (C) 2026 Pedro Veloso
 * SPDX-License-Identifier: Apache-2.0
 */
package com.pedronveloso.a11ybutton.data

import android.content.ComponentName
import android.content.Context
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Process
import com.pedronveloso.a11ybutton.model.AppSettings
import com.pedronveloso.a11ybutton.model.InstalledApp
import com.pedronveloso.a11ybutton.model.InvalidSelectionReason
import com.pedronveloso.a11ybutton.model.SelectedAppState
import java.text.Collator
import timber.log.Timber

class InstalledAppsRepository(
    private val context: Context,
) {
  private val packageManager = context.packageManager
  private val launcherApps = context.getSystemService(LauncherApps::class.java)
  private val collator = Collator.getInstance()

  fun getLaunchableApps(): List<InstalledApp> =
      launcherApps
          .getActivityList(null, Process.myUserHandle())
          .map { activityInfo ->
            InstalledApp(
                packageName = activityInfo.applicationInfo.packageName,
                componentName = activityInfo.componentName.flattenToString(),
                label = activityInfo.label?.toString().orEmpty().ifBlank { activityInfo.name },
            )
          }
          .sortedWith(compareBy(collator) { it.label.lowercase() })
          .also { Timber.d("Loaded %s launchable apps", it.size) }

  fun validateSelection(settings: AppSettings): SelectedAppState {
    val packageName = settings.selectedPackageName ?: return SelectedAppState.None
    val componentName = settings.selectedComponentName ?: return SelectedAppState.None
    Timber.d(
        "Validating saved selection for package=%s component=%s",
        packageName,
        componentName,
    )
    val component =
        ComponentName.unflattenFromString(componentName)
            ?: run {
              Timber.w("Saved component name is malformed: %s", componentName)
              return invalidSelection(settings, InvalidSelectionReason.NotLaunchable)
            }

    val activityInfo =
        try {
          packageManager.getActivityInfo(component, 0)
        } catch (exception: PackageManager.NameNotFoundException) {
          Timber.w(
              exception,
              "Saved activity no longer resolves for package=%s component=%s",
              packageName,
              componentName,
          )
          return when (resolveMissingComponentReason(packageName)) {
            InvalidSelectionReason.DisabledApp ->
                invalidSelection(settings, InvalidSelectionReason.DisabledApp)
            InvalidSelectionReason.MissingComponent ->
                invalidSelection(settings, InvalidSelectionReason.MissingComponent)
            else -> invalidSelection(settings, InvalidSelectionReason.MissingApp)
          }
        }

    if (!activityInfo.enabled || !activityInfo.applicationInfo.enabled) {
      Timber.w("Saved app is disabled for package=%s component=%s", packageName, componentName)
      return invalidSelection(settings, InvalidSelectionReason.DisabledApp)
    }

    val matchingApp = getLaunchableApps().firstOrNull { it.componentName == componentName }
    return if (matchingApp != null) {
      Timber.d("Saved selection is valid for component=%s", componentName)
      SelectedAppState.Valid(matchingApp)
    } else {
      Timber.w("Saved component is no longer launchable: %s", componentName)
      invalidSelection(settings, InvalidSelectionReason.NotLaunchable)
    }
  }

  fun loadIcon(componentName: String): Drawable? {
    val component =
        ComponentName.unflattenFromString(componentName)
            ?: run {
              Timber.w("Cannot load icon for malformed component name: %s", componentName)
              return null
            }
    return runCatching { packageManager.getActivityIcon(component) }
        .onFailure { exception ->
          Timber.w(exception, "Failed to load icon for component=%s", componentName)
        }
        .getOrNull()
  }

  private fun resolveMissingComponentReason(packageName: String): InvalidSelectionReason =
      try {
        val applicationInfo = packageManager.getApplicationInfo(packageName, 0)
        if (applicationInfo.enabled) {
          Timber.i("Saved package exists but launcher component changed: %s", packageName)
          InvalidSelectionReason.MissingComponent
        } else {
          Timber.i("Saved package is installed but disabled: %s", packageName)
          InvalidSelectionReason.DisabledApp
        }
      } catch (exception: PackageManager.NameNotFoundException) {
        Timber.w(exception, "Saved package is no longer installed: %s", packageName)
        InvalidSelectionReason.MissingApp
      }

  private fun invalidSelection(
      settings: AppSettings,
      reason: InvalidSelectionReason,
  ): SelectedAppState.Invalid =
      SelectedAppState.Invalid(
          packageName = settings.selectedPackageName,
          componentName = settings.selectedComponentName,
          reason = reason,
      )
}
