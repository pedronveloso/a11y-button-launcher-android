/*
 * Copyright (C) 2026 Pedro Veloso
 * SPDX-License-Identifier: Apache-2.0
 */
package com.pedronveloso.a11ybutton.ui.onboarding

import com.pedronveloso.a11ybutton.model.InstalledApp
import com.pedronveloso.a11ybutton.model.SelectedAppState
import com.pedronveloso.a11ybutton.model.ShortcutTarget
import com.pedronveloso.a11ybutton.ui.BackgroundProtectionState
import com.pedronveloso.a11ybutton.ui.MainScreenState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OnboardingStateTest {
  @Test
  fun steps_runInOrder_andStopAtBothEnds() {
    assertNull(OnboardingStep.Welcome.previous)
    assertEquals(OnboardingStep.Service, OnboardingStep.Welcome.next)
    assertEquals(OnboardingStep.Battery, OnboardingStep.Service.next)
    assertEquals(OnboardingStep.ButtonAction, OnboardingStep.Battery.next)
    assertNull(OnboardingStep.ButtonAction.next)
  }

  @Test
  fun isDone_followsEachRequirement() {
    val fresh = MainScreenState()
    OnboardingStep.entries.forEach { assertFalse(it.isDone(fresh)) }

    val configured =
        MainScreenState(
            disclosureAccepted = true,
            serviceEnabled = true,
            selectedAppState =
                SelectedAppState.Valid(InstalledApp("com.example.a", "com.example.a/.Main", "A")),
            backgroundProtection = BackgroundProtectionState(batteryOptimizationIgnored = true),
        )
    OnboardingStep.entries.forEach { assertTrue(it.isDone(configured)) }
  }

  @Test
  fun isDone_treatsAnInvalidSelectionAsNotDone() {
    val state =
        MainScreenState(
            selectedAppState =
                SelectedAppState.Invalid(
                    packageName = "com.example.a",
                    componentName = null,
                    reason = com.pedronveloso.a11ybutton.model.InvalidSelectionReason.MissingApp,
                ),
        )

    assertFalse(OnboardingStep.ButtonAction.isDone(state))
  }

  @Test
  fun buttonAction_acceptsAnAppShortcut_withoutRequiringOtherSetupSteps() {
    val state =
        MainScreenState(
            selectedAppState =
                SelectedAppState.ValidShortcut(
                    ShortcutTarget(
                        packageName = "com.example.mail",
                        shortcutId = "compose",
                        label = "Compose",
                        intentUri = "intent:#Intent;package=com.example.mail;end",
                    )
                )
        )

    assertTrue(OnboardingStep.ButtonAction.isDone(state))
    assertFalse(OnboardingStep.Welcome.isDone(state))
    assertFalse(OnboardingStep.Service.isDone(state))
    assertFalse(OnboardingStep.Battery.isDone(state))
  }
}
