/*
 * Copyright (C) 2026 Pedro Veloso
 * SPDX-License-Identifier: Apache-2.0
 */
package com.pedronveloso.a11ybutton.model

/** An app shortcut the button can launch, identified by the intent it fires. */
data class ShortcutTarget(
    val packageName: String,
    val shortcutId: String?,
    val label: String,
    /** [android.content.Intent.toUri] with `URI_INTENT_SCHEME`. */
    val intentUri: String,
) {
  /** Same shortcut regardless of label, which can change with the device language. */
  fun isSameShortcutAs(other: ShortcutTarget): Boolean =
      packageName == other.packageName &&
          shortcutId == other.shortcutId &&
          intentUri == other.intentUri
}
