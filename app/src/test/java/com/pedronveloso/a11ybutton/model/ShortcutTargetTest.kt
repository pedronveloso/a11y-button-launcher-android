/*
 * Copyright (C) 2026 Pedro Veloso
 * SPDX-License-Identifier: Apache-2.0
 */
package com.pedronveloso.a11ybutton.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShortcutTargetTest {
  private val compose =
      ShortcutTarget(
          packageName = "com.example.mail",
          shortcutId = "compose",
          label = "Compose",
          intentUri = "intent:#Intent;action=a;end",
      )

  @Test
  fun isSameShortcutAs_isFalse_whenOnlyTheIntentIsShared() {
    val other = compose.copy(shortcutId = "write", label = "Write")

    assertFalse(compose.isSameShortcutAs(other))
  }

  @Test
  fun isSameShortcutAs_ignoresTheLabel() {
    val relabeled = compose.copy(label = "Verfassen")

    assertTrue(compose.isSameShortcutAs(relabeled))
  }

  @Test
  fun isSameShortcutAs_fallsBackToTheIntent_whenThereIsNoId() {
    val created = compose.copy(shortcutId = null)

    assertTrue(created.isSameShortcutAs(created.copy(label = "Other")))
    assertFalse(created.isSameShortcutAs(created.copy(intentUri = "intent:#Intent;action=b;end")))
  }

  @Test
  fun isSameShortcutAs_isFalse_forDifferentPackages() {
    assertFalse(compose.isSameShortcutAs(compose.copy(packageName = "com.example.other")))
  }
}
