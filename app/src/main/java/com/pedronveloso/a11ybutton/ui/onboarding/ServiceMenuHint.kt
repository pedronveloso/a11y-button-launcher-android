/*
 * Copyright (C) 2026 Pedro Veloso
 * SPDX-License-Identifier: Apache-2.0
 */
package com.pedronveloso.a11ybutton.ui.onboarding

import com.pedronveloso.a11ybutton.ui.isXiaomiFamily
import com.pedronveloso.a11ybutton.ui.normalizedDeviceNames

/** Where a device's Accessibility settings list downloaded apps, so onboarding can point to it. */
enum class ServiceMenuHint {
  Xiaomi,
  Samsung,
  Pixel,
  Other;

  companion object {
    fun fromDevice(
        brand: String?,
        manufacturer: String?,
    ): ServiceMenuHint {
      val normalizedValues = normalizedDeviceNames(brand, manufacturer)

      return when {
        normalizedValues.isXiaomiFamily() -> Xiaomi
        normalizedValues.any { it.contains("samsung") } -> Samsung
        normalizedValues.any { it == "google" } -> Pixel
        else -> Other
      }
    }
  }
}
