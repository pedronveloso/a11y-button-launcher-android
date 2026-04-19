/*
 * Copyright (C) 2026 Pedro Veloso
 * SPDX-License-Identifier: Apache-2.0
 */
package com.pedronveloso.a11ybutton.model

import androidx.compose.runtime.Immutable

@Immutable
data class InstalledApp(
    val packageName: String,
    val componentName: String,
    val label: String,
)
