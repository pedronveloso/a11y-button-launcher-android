/*
 * Copyright (C) 2026 Pedro Veloso
 * All rights reserved.
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
