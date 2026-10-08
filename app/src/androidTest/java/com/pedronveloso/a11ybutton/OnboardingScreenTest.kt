/*
 * Copyright (C) 2026 Pedro Veloso
 * SPDX-License-Identifier: Apache-2.0
 */
package com.pedronveloso.a11ybutton

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pedronveloso.a11ybutton.model.InstalledApp
import com.pedronveloso.a11ybutton.model.SelectedAppState
import com.pedronveloso.a11ybutton.ui.BackgroundProtectionState
import com.pedronveloso.a11ybutton.ui.MainScreenState
import com.pedronveloso.a11ybutton.ui.onboarding.ONBOARDING_BACK_BUTTON_TAG
import com.pedronveloso.a11ybutton.ui.onboarding.ONBOARDING_PRIMARY_BUTTON_TAG
import com.pedronveloso.a11ybutton.ui.onboarding.ONBOARDING_SKIP_BUTTON_TAG
import com.pedronveloso.a11ybutton.ui.onboarding.OnboardingScreen
import com.pedronveloso.a11ybutton.ui.onboarding.OnboardingStep
import com.pedronveloso.a11ybutton.ui.theme.A11YButtonTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OnboardingScreenTest {
  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  private val app = InstalledApp("com.example.reader", "com.example.reader/.Home", "Reader")

  @Test
  fun welcomeStep_showsProgressAndAcceptsDisclosure() {
    var accepted = false
    setOnboarding(
        state = MainScreenState(),
        initialStep = OnboardingStep.Welcome,
        onAcceptDisclosure = { accepted = true },
    )

    composeTestRule.onNodeWithText("Step 1 of 4").assertIsDisplayed()
    composeTestRule.onNodeWithText("One button, one app").assertIsDisplayed()
    composeTestRule
        .onNodeWithText("The button belongs to Android itself", substring = true)
        .assertIsDisplayed()
    composeTestRule.onNodeWithTag(ONBOARDING_BACK_BUTTON_TAG).assertDoesNotExist()
    composeTestRule.onNodeWithText("Accept and continue").performClick()

    composeTestRule.runOnIdle { assertTrue(accepted) }
  }

  @Test
  fun serviceStep_opensAccessibilitySettings_andCanGoBack() {
    var settingsOpened = false
    var step = OnboardingStep.Service
    setOnboarding(
        state = MainScreenState(disclosureAccepted = true),
        initialStep = OnboardingStep.Service,
        onOpenAccessibilitySettings = { settingsOpened = true },
        onStepChanged = { step = it },
    )

    composeTestRule.onNodeWithText("Step 2 of 4").assertIsDisplayed()
    composeTestRule.onNodeWithText("Open Accessibility Settings").performClick()
    composeTestRule.onNodeWithTag(ONBOARDING_BACK_BUTTON_TAG).performClick()

    composeTestRule.runOnIdle {
      assertTrue(settingsOpened)
      assertEquals(OnboardingStep.Welcome, step)
    }
  }

  @Test
  fun completedStep_offersContinue_insteadOfItsAction() {
    var step = OnboardingStep.ButtonAction
    setOnboarding(
        state =
            MainScreenState(
                disclosureAccepted = true,
                serviceEnabled = true,
                selectedAppState = SelectedAppState.Valid(app),
            ),
        initialStep = OnboardingStep.ButtonAction,
        onStepChanged = { step = it },
    )

    composeTestRule.onNodeWithText("Done").assertIsDisplayed()
    composeTestRule.onNodeWithText("Choose what to open").assertDoesNotExist()
    composeTestRule.onNodeWithTag(ONBOARDING_PRIMARY_BUTTON_TAG).performClick()

    composeTestRule.runOnIdle { assertEquals(OnboardingStep.Battery, step) }
  }

  @Test
  fun serviceStep_advancesOnItsOwn_whenTheServiceGetsEnabled() {
    var step = OnboardingStep.Service
    var state by mutableStateOf(MainScreenState(disclosureAccepted = true))
    composeTestRule.setContent {
      A11YButtonTheme {
        var current by androidx.compose.runtime.remember { mutableStateOf(OnboardingStep.Service) }
        OnboardingScreen(
            screenState = state,
            step = current,
            onStepChange = {
              current = it
              step = it
            },
            onAcceptDisclosure = {},
            onOpenAccessibilitySettings = {},
            onChooseApp = {},
            onRequestBatteryExemption = {},
            onFinish = {},
        )
      }
    }

    composeTestRule.runOnUiThread { state = state.copy(serviceEnabled = true) }

    composeTestRule.runOnIdle { assertEquals(OnboardingStep.ButtonAction, step) }
  }

  @Test
  fun batteryStep_requestsExemption_thenFinishesWithAllSetState() {
    var requested = false
    var finished = false
    var state by mutableStateOf(allButBattery())
    composeTestRule.setContent {
      A11YButtonTheme {
        OnboardingScreen(
            screenState = state,
            step = OnboardingStep.Battery,
            onStepChange = {},
            onAcceptDisclosure = {},
            onOpenAccessibilitySettings = {},
            onChooseApp = {},
            onRequestBatteryExemption = { requested = true },
            onFinish = { finished = true },
        )
      }
    }

    composeTestRule.onNodeWithText("Step 4 of 4").assertIsDisplayed()
    composeTestRule.onNodeWithText("Allow Unrestricted Battery Access").performClick()
    composeTestRule.runOnIdle { assertTrue(requested) }

    composeTestRule.runOnUiThread {
      state =
          state.copy(
              backgroundProtection = BackgroundProtectionState(batteryOptimizationIgnored = true)
          )
    }

    composeTestRule.onNodeWithText("You're all set").assertIsDisplayed()
    composeTestRule.onNodeWithTag(ONBOARDING_SKIP_BUTTON_TAG).assertDoesNotExist()
    composeTestRule.onNodeWithTag(ONBOARDING_PRIMARY_BUTTON_TAG).performClick()
    composeTestRule.runOnIdle { assertTrue(finished) }
  }

  @Test
  fun skip_finishesOnboarding_fromAnyStep() {
    var finished = false
    setOnboarding(
        state = MainScreenState(),
        initialStep = OnboardingStep.Welcome,
        onFinish = { finished = true },
    )

    composeTestRule.onNodeWithTag(ONBOARDING_SKIP_BUTTON_TAG).performClick()

    composeTestRule.runOnIdle { assertTrue(finished) }
  }

  private fun allButBattery() =
      MainScreenState(
          disclosureAccepted = true,
          serviceEnabled = true,
          selectedAppState = SelectedAppState.Valid(app),
      )

  private fun setOnboarding(
      state: MainScreenState,
      initialStep: OnboardingStep,
      onAcceptDisclosure: () -> Unit = {},
      onOpenAccessibilitySettings: () -> Unit = {},
      onStepChanged: (OnboardingStep) -> Unit = {},
      onFinish: () -> Unit = {},
  ) {
    composeTestRule.setContent {
      A11YButtonTheme {
        var step by androidx.compose.runtime.remember { mutableStateOf(initialStep) }
        OnboardingScreen(
            screenState = state,
            step = step,
            onStepChange = {
              step = it
              onStepChanged(it)
            },
            onAcceptDisclosure = onAcceptDisclosure,
            onOpenAccessibilitySettings = onOpenAccessibilitySettings,
            onChooseApp = {},
            onRequestBatteryExemption = {},
            onFinish = onFinish,
        )
      }
    }
  }
}
