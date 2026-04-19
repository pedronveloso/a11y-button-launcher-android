/*
 * Copyright (C) 2026 Pedro Veloso
 * SPDX-License-Identifier: Apache-2.0
 */
package com.pedronveloso.a11ybutton.data

import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import timber.log.Timber

class AccessibilityStatusRepository(
    private val context: Context,
) {
  fun observeServiceEnabled(serviceComponent: ComponentName): Flow<Boolean> = flow {
    val enabled = isServiceEnabled(context, serviceComponent)
    Timber.d("Observed accessibility service enabled=%s", enabled)
    emit(enabled)
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
        Timber.d("Accessibility is globally disabled")
        return false
      }

      val enabledServices =
          Settings.Secure.getString(
              context.contentResolver,
              Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
          )

      return isEnabledServiceListed(enabledServices, serviceComponent.flattenToString()).also {
        Timber.d("Accessibility service %s enabled=%s", serviceComponent.flattenToString(), it)
      }
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
