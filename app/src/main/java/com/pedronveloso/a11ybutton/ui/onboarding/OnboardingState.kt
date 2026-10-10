/*
 * Copyright (C) 2026 Pedro Veloso
 * SPDX-License-Identifier: Apache-2.0
 */
package com.pedronveloso.a11ybutton.ui.onboarding

import com.pedronveloso.a11ybutton.model.isConfigured
import com.pedronveloso.a11ybutton.ui.MainScreenState

/** The first-run steps, in order. Each maps to one requirement of [MainScreenState]. */
enum class OnboardingStep {
  Welcome,
  Service,
  Battery,
  ButtonAction;

  val next: OnboardingStep?
    get() = entries.getOrNull(ordinal + 1)

  val previous: OnboardingStep?
    get() = entries.getOrNull(ordinal - 1)

  fun isDone(state: MainScreenState): Boolean =
      when (this) {
        Welcome -> state.disclosureAccepted
        Service -> state.serviceEnabled
        ButtonAction -> state.selectedAppState.isConfigured
        Battery -> state.backgroundProtection.batteryOptimizationIgnored
      }
}
