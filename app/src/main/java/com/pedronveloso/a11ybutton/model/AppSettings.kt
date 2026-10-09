/*
 * Copyright (C) 2026 Pedro Veloso
 * SPDX-License-Identifier: Apache-2.0
 */
package com.pedronveloso.a11ybutton.model

data class AppSettings(
    val loggingCleanupPending: Boolean = false,
    val inAppLoggingEnabled: Boolean = com.pedronveloso.a11ybutton.BuildConfig.DEBUG,
    val selectedPackageName: String? = null,
    val selectedComponentName: String? = null,
    val selectedShortcut: ShortcutTarget? = null,
    val disclosureAccepted: Boolean = false,
    val xiaomiRecentsLockConfirmed: Boolean = false,
    val notificationPreference: NotificationPreference = NotificationPreference.Disabled,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val onboardingCompleted: Boolean = false,
)
