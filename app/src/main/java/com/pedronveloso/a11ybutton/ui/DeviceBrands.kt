/*
 * Copyright (C) 2026 Pedro Veloso
 * SPDX-License-Identifier: Apache-2.0
 */
package com.pedronveloso.a11ybutton.ui

private val XIAOMI_FAMILY = listOf("xiaomi", "redmi", "poco")

/** Lowercased, trimmed [brand] and [manufacturer], for matching against known device names. */
internal fun normalizedDeviceNames(brand: String?, manufacturer: String?): List<String> =
    listOfNotNull(brand, manufacturer).map { value -> value.trim().lowercase() }

/** Xiaomi, Redmi and POCO devices share the same software and quirks. */
internal fun List<String>.isXiaomiFamily(): Boolean = any { name ->
  XIAOMI_FAMILY.any { name.contains(it) }
}
