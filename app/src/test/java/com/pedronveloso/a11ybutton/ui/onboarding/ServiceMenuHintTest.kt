/*
 * Copyright (C) 2026 Pedro Veloso
 * SPDX-License-Identifier: Apache-2.0
 */
package com.pedronveloso.a11ybutton.ui.onboarding

import org.junit.Assert.assertEquals
import org.junit.Test

class ServiceMenuHintTest {
  @Test
  fun fromDevice_detectsXiaomi_includingSubBrands() {
    assertEquals(ServiceMenuHint.Xiaomi, ServiceMenuHint.fromDevice("Xiaomi", "Xiaomi"))
    assertEquals(ServiceMenuHint.Xiaomi, ServiceMenuHint.fromDevice("Redmi", "Xiaomi"))
    assertEquals(ServiceMenuHint.Xiaomi, ServiceMenuHint.fromDevice("POCO", "Xiaomi"))
    assertEquals(ServiceMenuHint.Xiaomi, ServiceMenuHint.fromDevice("Redmi", null))
    assertEquals(ServiceMenuHint.Xiaomi, ServiceMenuHint.fromDevice("poco", "poco"))
  }

  @Test
  fun fromDevice_detectsSamsung() {
    assertEquals(ServiceMenuHint.Samsung, ServiceMenuHint.fromDevice("samsung", "samsung"))
  }

  @Test
  fun fromDevice_detectsPixel() {
    assertEquals(ServiceMenuHint.Pixel, ServiceMenuHint.fromDevice("google", "Google"))
  }

  @Test
  fun fromDevice_fallsBackToOther() {
    assertEquals(ServiceMenuHint.Other, ServiceMenuHint.fromDevice("OnePlus", "OnePlus"))
    assertEquals(ServiceMenuHint.Other, ServiceMenuHint.fromDevice(null, null))
    assertEquals(ServiceMenuHint.Other, ServiceMenuHint.fromDevice(" ", ""))
  }
}
