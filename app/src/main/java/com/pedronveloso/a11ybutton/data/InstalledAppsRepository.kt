/*
 * Copyright (C) 2026 Pedro Veloso
 * All rights reserved.
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
            }.sortedWith(compareBy(collator) { it.label.lowercase() })

    fun validateSelection(settings: AppSettings): SelectedAppState {
        val packageName = settings.selectedPackageName ?: return SelectedAppState.None
        val componentName = settings.selectedComponentName ?: return SelectedAppState.None
        val component = ComponentName.unflattenFromString(componentName)
            ?: return invalidSelection(settings, InvalidSelectionReason.NotLaunchable)

        val activityInfo =
            try {
                packageManager.getActivityInfo(component, 0)
            } catch (_: PackageManager.NameNotFoundException) {
                return when (resolveMissingComponentReason(packageName)) {
                    InvalidSelectionReason.DisabledApp ->
                        invalidSelection(settings, InvalidSelectionReason.DisabledApp)
                    InvalidSelectionReason.MissingComponent ->
                        invalidSelection(settings, InvalidSelectionReason.MissingComponent)
                    else -> invalidSelection(settings, InvalidSelectionReason.MissingApp)
                }
            }

        if (!activityInfo.enabled || !activityInfo.applicationInfo.enabled) {
            return invalidSelection(settings, InvalidSelectionReason.DisabledApp)
        }

        val matchingApp = getLaunchableApps().firstOrNull { it.componentName == componentName }
        return if (matchingApp != null) {
            SelectedAppState.Valid(matchingApp)
        } else {
            invalidSelection(settings, InvalidSelectionReason.NotLaunchable)
        }
    }

    fun loadIcon(componentName: String): Drawable? {
        val component = ComponentName.unflattenFromString(componentName) ?: return null
        return runCatching { packageManager.getActivityIcon(component) }.getOrNull()
    }

    private fun resolveMissingComponentReason(packageName: String): InvalidSelectionReason =
        try {
            val applicationInfo = packageManager.getApplicationInfo(packageName, 0)
            if (applicationInfo.enabled) {
                InvalidSelectionReason.MissingComponent
            } else {
                InvalidSelectionReason.DisabledApp
            }
        } catch (_: PackageManager.NameNotFoundException) {
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
