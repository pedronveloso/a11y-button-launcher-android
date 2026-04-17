/*
 * Copyright (C) 2026 Pedro Veloso
 * All rights reserved.
 */
package com.pedronveloso.a11ybutton.ui

import androidx.compose.runtime.Immutable
import com.pedronveloso.a11ybutton.model.InstalledApp

@Immutable
data class AppPickerApps(
    val items: List<InstalledApp> = emptyList(),
)
