/*
 * Copyright (C) 2026 Pedro Veloso
 * SPDX-License-Identifier: Apache-2.0
 */
package com.pedronveloso.a11ybutton.model

data class AppSettings(
    val selectedPackageName: String? = null,
    val selectedComponentName: String? = null,
    val disclosureAccepted: Boolean = false,
    val xiaomiRecentsLockConfirmed: Boolean = false,
    val notificationsOptedOut: Boolean = false,
)
