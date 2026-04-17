/*
 * Copyright (C) 2026 Pedro Veloso
 * All rights reserved.
 */
package com.pedronveloso.a11ybutton.model

data class AppSettings(
    val selectedPackageName: String? = null,
    val selectedComponentName: String? = null,
    val disclosureAccepted: Boolean = false,
)
