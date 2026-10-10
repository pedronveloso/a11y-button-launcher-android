/*
 * Copyright (C) 2026 Pedro Veloso
 * SPDX-License-Identifier: Apache-2.0
 */
package com.pedronveloso.a11ybutton.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BackgroundProtectionBrandTest {
  @Test
  fun fromDevice_treatsXiaomiRedmiAndPocoAsXiaomi() {
    assertEquals(
        BackgroundProtectionBrand.Xiaomi,
        BackgroundProtectionBrand.fromDevice("Xiaomi", "Xiaomi"),
    )
    assertEquals(
        BackgroundProtectionBrand.Xiaomi,
        BackgroundProtectionBrand.fromDevice("Redmi", null),
    )
    assertEquals(
        BackgroundProtectionBrand.Xiaomi,
        BackgroundProtectionBrand.fromDevice("POCO", "other"),
    )
  }

  @Test
  fun fromDevice_detectsHuawei_andIgnoresOtherBrands() {
    assertEquals(
        BackgroundProtectionBrand.Huawei,
        BackgroundProtectionBrand.fromDevice("HUAWEI", "HUAWEI"),
    )
    assertNull(BackgroundProtectionBrand.fromDevice("google", "Google"))
  }
}
