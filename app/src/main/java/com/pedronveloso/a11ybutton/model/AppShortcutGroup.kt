/*
 * Copyright (C) 2026 Pedro Veloso
 * SPDX-License-Identifier: Apache-2.0
 */
package com.pedronveloso.a11ybutton.model

import androidx.compose.runtime.Immutable

/** One row under an app in the shortcut picker. */
sealed interface ShortcutEntry {
  val label: String

  /** A static shortcut declared in the app's manifest, ready to be saved. */
  data class Ready(
      val target: ShortcutTarget,
  ) : ShortcutEntry {
    override val label: String
      get() = target.label
  }

  /**
   * An activity the app exposes to let the user create a shortcut
   * ([android.content.Intent.ACTION_CREATE_SHORTCUT]).
   */
  data class Creator(
      val packageName: String,
      val componentName: String,
      override val label: String,
  ) : ShortcutEntry
}

/** An app together with the shortcuts it offers. [app] is used for its label and icon. */
data class AppShortcutGroup(
    val app: InstalledApp,
    val entries: List<ShortcutEntry>,
)

@Immutable
data class AppPickerShortcuts(
    val groups: List<AppShortcutGroup> = emptyList(),
    val isLoading: Boolean = false,
)
