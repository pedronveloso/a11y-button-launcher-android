/*
 * Copyright (C) 2026 Pedro Veloso
 * SPDX-License-Identifier: Apache-2.0
 */
package com.pedronveloso.a11ybutton.ui

import com.pedronveloso.a11ybutton.model.InstalledApp
import com.pedronveloso.a11ybutton.model.InvalidSelectionReason
import com.pedronveloso.a11ybutton.model.NotificationPreference
import com.pedronveloso.a11ybutton.model.SelectedAppState
import org.junit.Assert.assertEquals
import org.junit.Test

class MainScreenStateTest {
  @Test
  fun deriveMainScreenState_isNotSetUp_whenNothingIsConfigured() {
    val state =
        deriveMainScreenState(
            serviceEnabled = false,
            disclosureAccepted = false,
            selectedAppState = SelectedAppState.None,
            serviceMessage = null,
        )

    assertEquals(SetupReadiness.NotSetUp, state.readiness)
  }

  @Test
  fun deriveMainScreenState_isPartiallySetUp_whenOnlyServiceIsEnabled() {
    val state =
        deriveMainScreenState(
            serviceEnabled = true,
            disclosureAccepted = true,
            selectedAppState = SelectedAppState.None,
            serviceMessage = null,
        )

    assertEquals(SetupReadiness.PartiallySetUp, state.readiness)
  }

  @Test
  fun deriveMainScreenState_isPartiallySetUp_whenDisclosureIsMissing() {
    val state =
        deriveMainScreenState(
            serviceEnabled = true,
            disclosureAccepted = false,
            selectedAppState =
                SelectedAppState.Valid(
                    app =
                        InstalledApp(
                            packageName = "com.example.reader",
                            componentName = "com.example.reader/.HomeActivity",
                            label = "Reader",
                        ),
                ),
            serviceMessage = null,
        )

    assertEquals(SetupReadiness.PartiallySetUp, state.readiness)
  }

  @Test
  fun deriveMainScreenState_isReady_whenServiceAndSelectionExist() {
    val state =
        deriveMainScreenState(
            serviceEnabled = true,
            disclosureAccepted = true,
            selectedAppState =
                SelectedAppState.Valid(
                    app =
                        InstalledApp(
                            packageName = "com.example.reader",
                            componentName = "com.example.reader/.HomeActivity",
                            label = "Reader",
                        ),
                ),
            serviceMessage = null,
        )

    assertEquals(SetupReadiness.Ready, state.readiness)
  }

  @Test
  fun deriveMainScreenState_isNotReady_whenSelectionIsInvalid() {
    val state =
        deriveMainScreenState(
            serviceEnabled = true,
            disclosureAccepted = true,
            selectedAppState =
                SelectedAppState.Invalid(
                    packageName = "com.example.reader",
                    componentName = "com.example.reader/.HomeActivity",
                    reason = InvalidSelectionReason.MissingApp,
                ),
            serviceMessage = null,
        )

    assertEquals(SetupReadiness.PartiallySetUp, state.readiness)
  }

  @Test
  fun deriveMainScreenState_isPartiallySetUp_whenXiaomiBackgroundProtectionIsIncomplete() {
    val state =
        deriveMainScreenState(
            serviceEnabled = true,
            disclosureAccepted = true,
            selectedAppState =
                SelectedAppState.Valid(
                    app =
                        InstalledApp(
                            packageName = "com.example.reader",
                            componentName = "com.example.reader/.HomeActivity",
                            label = "Reader",
                        ),
                ),
            backgroundProtection =
                BackgroundProtectionState(
                    requiredBrand = BackgroundProtectionBrand.Xiaomi,
                    batteryOptimizationIgnored = true,
                    recentsLockConfirmed = false,
                ),
            serviceMessage = null,
        )

    assertEquals(SetupReadiness.PartiallySetUp, state.readiness)
  }

  @Test
  fun deriveMainScreenState_marksNotificationsEnabled_whenPreferenceIsEnabled() {
    val state =
        deriveMainScreenState(
            serviceEnabled = false,
            disclosureAccepted = false,
            selectedAppState = SelectedAppState.None,
            notificationPreference = NotificationPreference.Enabled,
        )

    assertEquals(true, state.notificationsEnabled)
  }

  @Test
  fun deriveMainScreenState_marksNotificationsDisabled_whenPreferenceIsOptedOut() {
    val state =
        deriveMainScreenState(
            serviceEnabled = false,
            disclosureAccepted = false,
            selectedAppState = SelectedAppState.None,
            notificationPreference = NotificationPreference.OptedOut,
        )

    assertEquals(false, state.notificationsEnabled)
  }

  @Test
  fun deriveMainScreenState_isReady_whenHuaweiBackgroundProtectionIsComplete() {
    val state =
        deriveMainScreenState(
            serviceEnabled = true,
            disclosureAccepted = true,
            selectedAppState =
                SelectedAppState.Valid(
                    app =
                        InstalledApp(
                            packageName = "com.example.reader",
                            componentName = "com.example.reader/.HomeActivity",
                            label = "Reader",
                        ),
                ),
            backgroundProtection =
                BackgroundProtectionState(
                    requiredBrand = BackgroundProtectionBrand.Huawei,
                    batteryOptimizationIgnored = true,
                ),
            serviceMessage = null,
        )

    assertEquals(SetupReadiness.Ready, state.readiness)
  }
}
