/*
 * Copyright (C) 2026 Pedro Veloso
 * All rights reserved.
 */
package com.pedronveloso.a11ybutton.model

import androidx.compose.runtime.Immutable

@Immutable
data class InstalledApp(
    val packageName: String,
    val componentName: String,
    val label: String,
)
