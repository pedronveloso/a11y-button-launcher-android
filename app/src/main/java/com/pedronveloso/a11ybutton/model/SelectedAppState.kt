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
}

sealed interface SelectedAppState {
  data object None : SelectedAppState

  data class Valid(
      val app: InstalledApp,
  ) : SelectedAppState

  data class Invalid(
      val packageName: String?,
      val componentName: String?,
      val reason: InvalidSelectionReason,
  ) : SelectedAppState
}
