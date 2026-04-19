/*
 * Copyright (C) 2026 Pedro Veloso
 * SPDX-License-Identifier: Apache-2.0
 */
package com.pedronveloso.a11ybutton.ui

import androidx.compose.runtime.Immutable
import com.pedronveloso.a11ybutton.model.InstalledApp

@Immutable
data class AppPickerApps(
    val items: List<InstalledApp> = emptyList(),
)
