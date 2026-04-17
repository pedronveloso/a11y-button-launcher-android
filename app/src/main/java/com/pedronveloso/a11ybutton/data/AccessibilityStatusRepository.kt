/*
 * Copyright (C) 2026 Pedro Veloso
 * All rights reserved.
 */
package com.pedronveloso.a11ybutton.data

import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class AccessibilityStatusRepository(
    private val context: Context,
) {
  fun observeServiceEnabled(serviceComponent: ComponentName): Flow<Boolean> = flow {
    emit(isServiceEnabled(context, serviceComponent))
  }

  companion object {
    fun isServiceEnabled(
        context: Context,
        serviceComponent: ComponentName,
    ): Boolean {
      val accessibilityEnabled =
          Settings.Secure.getInt(
              context.contentResolver,
              Settings.Secure.ACCESSIBILITY_ENABLED,
              0,
          ) == 1

      if (!accessibilityEnabled) {
        return false
      }

      val enabledServices =
          Settings.Secure.getString(
              context.contentResolver,
              Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
          )

      return isEnabledServiceListed(enabledServices, serviceComponent.flattenToString())
    }

    internal fun isEnabledServiceListed(
        enabledServices: String?,
        serviceId: String,
    ): Boolean =
        enabledServices?.split(':')?.map(String::trim)?.any {
          it.equals(serviceId, ignoreCase = true)
        } ?: false
  }
}
