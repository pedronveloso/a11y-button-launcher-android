/*
 * Copyright (C) 2026 Pedro Veloso
 * SPDX-License-Identifier: Apache-2.0
 */
package com.pedronveloso.a11ybutton.ui.onboarding

/** Where a device's Accessibility settings list downloaded apps, so onboarding can point to it. */
enum class ServiceMenuHint {
  Xiaomi,
  Samsung,
  Pixel,
  Other;

  companion object {
    private val XIAOMI_FAMILY = listOf("xiaomi", "redmi", "poco")

    fun fromDevice(
        brand: String?,
        manufacturer: String?,
    ): ServiceMenuHint {
      val normalizedValues =
          listOfNotNull(brand, manufacturer).map { value -> value.trim().lowercase() }

      return when {
        normalizedValues.any { value -> XIAOMI_FAMILY.any { value.contains(it) } } -> Xiaomi
        normalizedValues.any { it.contains("samsung") } -> Samsung
        normalizedValues.any { it == "google" } -> Pixel
        else -> Other
      }
    }
  }
}
