/*
 * Copyright (C) 2026 Pedro Veloso
 * SPDX-License-Identifier: Apache-2.0
 */
package com.pedronveloso.a11ybutton.ui

import com.pedronveloso.a11ybutton.model.NotificationPreference
import com.pedronveloso.a11ybutton.model.SelectedAppState
import com.pedronveloso.a11ybutton.model.isConfigured
import com.pedronveloso.a11ybutton.ui.onboarding.ServiceMenuHint

enum class SetupReadiness {
  NotSetUp,
  PartiallySetUp,
  Ready,
}

data class MainScreenState(
    val serviceEnabled: Boolean = false,
    val disclosureAccepted: Boolean = false,
    val selectedAppState: SelectedAppState = SelectedAppState.None,
    val backgroundProtection: BackgroundProtectionState = BackgroundProtectionState(),
    val serviceMessage: String? = null,
    val readiness: SetupReadiness = SetupReadiness.NotSetUp,
    val notificationPreference: NotificationPreference = NotificationPreference.Disabled,
    val serviceMenuHint: ServiceMenuHint = ServiceMenuHint.Other,
) {
  val isReady: Boolean
    get() = readiness == SetupReadiness.Ready

  val notificationsEnabled: Boolean
    get() = notificationPreference == NotificationPreference.Enabled
}

fun deriveMainScreenState(
    serviceEnabled: Boolean,
    disclosureAccepted: Boolean,
    selectedAppState: SelectedAppState,
    backgroundProtection: BackgroundProtectionState = BackgroundProtectionState(),
    serviceMessage: String? = null,
    notificationPreference: NotificationPreference = NotificationPreference.Disabled,
    serviceMenuHint: ServiceMenuHint = ServiceMenuHint.Other,
): MainScreenState {
  val selectedAppConfigured = selectedAppState.isConfigured
  val requirements = buildList {
    add(serviceEnabled)
    add(disclosureAccepted)
    add(selectedAppConfigured)
    add(backgroundProtection.isComplete)
  }
  val completedRequirements = requirements.count { it }
  val readiness =
      when (completedRequirements) {
        requirements.size -> SetupReadiness.Ready
        0 -> SetupReadiness.NotSetUp
        else -> SetupReadiness.PartiallySetUp
      }

  return MainScreenState(
      serviceEnabled = serviceEnabled,
      disclosureAccepted = disclosureAccepted,
      selectedAppState = selectedAppState,
      backgroundProtection = backgroundProtection,
      serviceMessage = serviceMessage,
      readiness = readiness,
      notificationPreference = notificationPreference,
      serviceMenuHint = serviceMenuHint,
  )
}
