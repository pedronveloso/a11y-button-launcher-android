/*
 * Copyright (C) 2026 Pedro Veloso
 * SPDX-License-Identifier: Apache-2.0
 */
package com.pedronveloso.a11ybutton.ui

enum class BackgroundProtectionBrand {
  Xiaomi,
  Huawei;

  companion object {
    fun fromDevice(
        brand: String?,
        manufacturer: String?,
    ): BackgroundProtectionBrand? {
      val normalizedValues =
          listOfNotNull(brand, manufacturer).map { value -> value.trim().lowercase() }

      return when {
        normalizedValues.any { it.contains("xiaomi") } -> Xiaomi
        normalizedValues.any { it.contains("huawei") } -> Huawei
        else -> null
      }
    }
  }
}

data class BackgroundProtectionState(
    val requiredBrand: BackgroundProtectionBrand? = null,
    val batteryOptimizationIgnored: Boolean = false,
    val recentsLockConfirmed: Boolean = false,
) {
  val isRequired: Boolean
    get() = requiredBrand != null

  val requiresRecentsLock: Boolean
    get() = requiredBrand == BackgroundProtectionBrand.Xiaomi

  val isComplete: Boolean
    get() =
        !isRequired ||
            (batteryOptimizationIgnored && (!requiresRecentsLock || recentsLockConfirmed))
}
