/*
 * Copyright (C) 2026 Pedro Veloso
 * SPDX-License-Identifier: Apache-2.0
 */
package com.pedronveloso.a11ybutton.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeviceBrandsTest {
  @Test
  fun normalizedDeviceNames_trimsLowercasesAndDropsNulls() {
    assertEquals(listOf("redmi", "xiaomi"), normalizedDeviceNames(" Redmi ", "XIAOMI"))
    assertEquals(listOf("google"), normalizedDeviceNames(null, "Google"))
    assertEquals(emptyList<String>(), normalizedDeviceNames(null, null))
  }

  @Test
  fun isXiaomiFamily_matchesXiaomiRedmiAndPoco() {
    assertTrue(listOf("xiaomi").isXiaomiFamily())
    assertTrue(listOf("redmi", "other").isXiaomiFamily())
    assertTrue(listOf("poco").isXiaomiFamily())
    assertFalse(listOf("samsung", "google").isXiaomiFamily())
    assertFalse(emptyList<String>().isXiaomiFamily())
  }
}
