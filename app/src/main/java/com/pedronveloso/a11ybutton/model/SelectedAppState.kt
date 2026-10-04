/*
 * Copyright (C) 2026 Pedro Veloso
 * SPDX-License-Identifier: Apache-2.0
 */
package com.pedronveloso.a11ybutton.model

enum class InvalidSelectionReason {
  MissingApp,
  DisabledApp,
  MissingComponent,
  NotLaunchable,
  ShortcutNotResolvable,
}

sealed interface SelectedAppState {
  data object None : SelectedAppState

  data class Valid(
      val app: InstalledApp,
  ) : SelectedAppState

  data class ValidShortcut(
      val shortcut: ShortcutTarget,
  ) : SelectedAppState

  data class Invalid(
      val packageName: String?,
      val componentName: String?,
      val reason: InvalidSelectionReason,
  ) : SelectedAppState
}

/** True when the button has a working target, whether an app or an app shortcut. */
val SelectedAppState.isConfigured: Boolean
  get() = this is SelectedAppState.Valid || this is SelectedAppState.ValidShortcut
