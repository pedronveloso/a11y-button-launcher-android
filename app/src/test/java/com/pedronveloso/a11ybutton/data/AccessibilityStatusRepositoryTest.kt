/*
 * Copyright (C) 2026 Pedro Veloso
 * SPDX-License-Identifier: Apache-2.0
 */
package com.pedronveloso.a11ybutton.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AccessibilityStatusRepositoryTest {
  @Test
  fun isEnabledServiceListed_returnsTrue_whenServiceIdExists() {
    val enabledServices =
        "com.example.reader/.ReaderService:com.pedronveloso.a11ybutton/.service.ShortcutLaunchAccessibilityService"

    val isEnabled =
        AccessibilityStatusRepository.isEnabledServiceListed(
            enabledServices = enabledServices,
            serviceId = "com.pedronveloso.a11ybutton/.service.ShortcutLaunchAccessibilityService",
        )

    assertTrue(isEnabled)
  }

  @Test
  fun isEnabledServiceListed_returnsFalse_whenListIsMissingService() {
    val isEnabled =
        AccessibilityStatusRepository.isEnabledServiceListed(
            enabledServices = "com.example.reader/.ReaderService",
            serviceId = "com.pedronveloso.a11ybutton/.service.ShortcutLaunchAccessibilityService",
        )

    assertFalse(isEnabled)
  }

  @Test
  fun isEnabledServiceListed_returnsFalse_whenValueIsNull() {
    val isEnabled =
        AccessibilityStatusRepository.isEnabledServiceListed(
            enabledServices = null,
            serviceId = "com.pedronveloso.a11ybutton/.service.ShortcutLaunchAccessibilityService",
        )

    assertFalse(isEnabled)
  }
}
