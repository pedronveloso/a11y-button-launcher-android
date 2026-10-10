/*
 * Copyright (C) 2026 Pedro Veloso
 * SPDX-License-Identifier: Apache-2.0
 */
package com.pedronveloso.a11ybutton

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.espresso.Espresso.pressBack
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pedronveloso.a11ybutton.model.InstalledApp
import com.pedronveloso.a11ybutton.model.SelectedAppState
import com.pedronveloso.a11ybutton.ui.BackgroundProtectionBrand
import com.pedronveloso.a11ybutton.ui.BackgroundProtectionState
import com.pedronveloso.a11ybutton.ui.MainScreenState
import com.pedronveloso.a11ybutton.ui.onboarding.ONBOARDING_BACK_BUTTON_TAG
import com.pedronveloso.a11ybutton.ui.onboarding.ONBOARDING_PRIMARY_BUTTON_TAG
import com.pedronveloso.a11ybutton.ui.onboarding.ONBOARDING_SKIP_ACTION_BUTTON_TAG
import com.pedronveloso.a11ybutton.ui.onboarding.ONBOARDING_SKIP_BUTTON_TAG
import com.pedronveloso.a11ybutton.ui.onboarding.OnboardingScreen
import com.pedronveloso.a11ybutton.ui.onboarding.OnboardingStep
import com.pedronveloso.a11ybutton.ui.onboarding.ServiceMenuHint
import com.pedronveloso.a11ybutton.ui.theme.A11YButtonTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
        .performScrollTo()
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithContentDescription(
            "Accessibility button at the bottom right of Android’s navigation bar"
        )
        .performScrollTo()
        .assertIsDisplayed()
    composeTestRule.onNodeWithText("Navigation bar button").performScrollTo().assertIsDisplayed()
    composeTestRule
        .onNodeWithContentDescription(
            "Accessibility button floating at the right edge of the screen"
        )
        .performScrollTo()
        .assertIsDisplayed()
    composeTestRule.onNodeWithText("Floating button").performScrollTo().assertIsDisplayed()
    composeTestRule
        .onNodeWithText(composeTestRule.activity.getString(R.string.main_disclosure_body))
        .performScrollTo()
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
    var step = OnboardingStep.Battery
    setOnboarding(
        state = allSet().copy(selectedAppState = SelectedAppState.None),
        initialStep = OnboardingStep.Battery,
        onStepChanged = { step = it },
    )

    composeTestRule.onNodeWithText("Continue").assertIsDisplayed()
    composeTestRule.onNodeWithText("Allow Unrestricted Battery Access").assertDoesNotExist()
    composeTestRule.onNodeWithTag(ONBOARDING_PRIMARY_BUTTON_TAG).performClick()

    composeTestRule.runOnIdle { assertEquals(OnboardingStep.ButtonAction, step) }
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

    composeTestRule.runOnIdle { assertEquals(OnboardingStep.Battery, step) }
  }

  @Test
  fun welcomeStep_acceptanceAdvancesToService_withoutFinishingOnboarding() {
    var state by mutableStateOf(MainScreenState())
    var finished = false
    composeTestRule.setContent {
      A11YButtonTheme {
        var step by rememberSaveable { mutableStateOf(OnboardingStep.Welcome) }
        OnboardingScreen(
            screenState = state,
            step = step,
            onStepChange = { step = it },
            onAcceptDisclosure = { state = state.copy(disclosureAccepted = true) },
            onOpenAccessibilitySettings = {},
            onChooseApp = {},
            onRequestBatteryExemption = {},
            onFinish = { finished = true },
        )
      }
    }

    composeTestRule.onNodeWithTag(ONBOARDING_PRIMARY_BUTTON_TAG).performClick()

    composeTestRule.onNodeWithText("Step 2 of 4").assertIsDisplayed()
    composeTestRule.onNodeWithText("Open Accessibility Settings").assertIsDisplayed()
    composeTestRule.runOnIdle { assertFalse(finished) }
  }

  @Test
  fun completedActionStep_showsRemainingXiaomiSetup_andLetsUserGoBack() {
    var step = OnboardingStep.ButtonAction
    setOnboarding(
        state =
            allSet()
                .copy(
                    backgroundProtection =
                        BackgroundProtectionState(
                            requiredBrand = BackgroundProtectionBrand.Xiaomi,
                            batteryOptimizationIgnored = true,
                            recentsLockConfirmed = false,
                        )
                ),
        initialStep = OnboardingStep.ButtonAction,
        onStepChanged = { step = it },
    )

    composeTestRule.onNodeWithText("You're all set").assertIsDisplayed()
    composeTestRule
        .onNodeWithText(composeTestRule.activity.getString(R.string.onboarding_complete_xiaomi))
        .performScrollTo()
        .assertIsDisplayed()
    composeTestRule.onNodeWithTag(ONBOARDING_SKIP_ACTION_BUTTON_TAG).assertDoesNotExist()
    composeTestRule.onNodeWithTag(ONBOARDING_BACK_BUTTON_TAG).performClick()

    composeTestRule.runOnIdle { assertEquals(OnboardingStep.Battery, step) }
    composeTestRule.onNodeWithText("Step 3 of 4").assertIsDisplayed()
  }

  @Test
  fun batteryStep_requestsExemption_thenOffersContinue() {
    var requested = false
    var state by mutableStateOf(allSet().copy(backgroundProtection = BackgroundProtectionState()))
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
            onFinish = {},
        )
      }
    }

    composeTestRule.onNodeWithText("Step 3 of 4").assertIsDisplayed()
    composeTestRule.onNodeWithText("Allow Unrestricted Battery Access").performClick()
    composeTestRule.runOnIdle { assertTrue(requested) }

    composeTestRule.runOnUiThread {
      state =
          state.copy(
              backgroundProtection = BackgroundProtectionState(batteryOptimizationIgnored = true)
          )
    }

    composeTestRule.onNodeWithText("Continue").assertIsDisplayed()
    composeTestRule.onNodeWithText("You're all set").assertDoesNotExist()
  }

  @Test
  fun actionStep_chooseApp_thenFinishesWithAllSetState() {
    var chose = false
    var finished = false
    var state by mutableStateOf(allSet().copy(selectedAppState = SelectedAppState.None))
    composeTestRule.setContent {
      A11YButtonTheme {
        OnboardingScreen(
            screenState = state,
            step = OnboardingStep.ButtonAction,
            onStepChange = {},
            onAcceptDisclosure = {},
            onOpenAccessibilitySettings = {},
            onChooseApp = { chose = true },
            onRequestBatteryExemption = {},
            onFinish = { finished = true },
        )
      }
    }

    composeTestRule.onNodeWithText("Step 4 of 4").assertIsDisplayed()
    composeTestRule.onNodeWithTag(ONBOARDING_PRIMARY_BUTTON_TAG).performClick()
    composeTestRule.runOnIdle { assertTrue(chose) }

    composeTestRule.runOnUiThread {
      state = state.copy(selectedAppState = SelectedAppState.Valid(app))
    }

    composeTestRule.onNodeWithText("You're all set").assertIsDisplayed()
    composeTestRule.onNodeWithTag(ONBOARDING_SKIP_BUTTON_TAG).assertDoesNotExist()
    composeTestRule.onNodeWithTag(ONBOARDING_SKIP_ACTION_BUTTON_TAG).assertDoesNotExist()
    composeTestRule.onNodeWithTag(ONBOARDING_PRIMARY_BUTTON_TAG).performClick()
    composeTestRule.runOnIdle { assertTrue(finished) }
  }

  @Test
  fun actionStep_skipForNow_finishesWithoutAnApp() {
    var finished = false
    setOnboarding(
        state = allSet().copy(selectedAppState = SelectedAppState.None),
        initialStep = OnboardingStep.ButtonAction,
        onFinish = { finished = true },
    )

    composeTestRule.onNodeWithTag(ONBOARDING_SKIP_BUTTON_TAG).assertDoesNotExist()
    composeTestRule.onNodeWithTag(ONBOARDING_SKIP_ACTION_BUTTON_TAG).performClick()

    composeTestRule.runOnIdle { assertTrue(finished) }
  }

  @Test
  fun serviceStep_pointsToTheDownloadedAppsMenu_onXiaomi() {
    setOnboarding(
        state = MainScreenState(),
        initialStep = OnboardingStep.Service,
        serviceMenuHint = ServiceMenuHint.Xiaomi,
    )

    composeTestRule.onNodeWithText("Downloaded apps", substring = true).assertIsDisplayed()
  }

  @Test
  fun skipForNowButton_isOnlyOnTheActionStep() {
    setOnboarding(state = allSet(), initialStep = OnboardingStep.Service)

    composeTestRule.onNodeWithTag(ONBOARDING_SKIP_ACTION_BUTTON_TAG).assertDoesNotExist()
  }

  @Test
  fun skip_requiresConfirmation_beforeFinishingOnboarding() {
    var finished = false
    setOnboarding(
        state = MainScreenState(),
        initialStep = OnboardingStep.Welcome,
        onFinish = { finished = true },
    )

    composeTestRule.onNodeWithTag(ONBOARDING_SKIP_BUTTON_TAG).performClick()
    composeTestRule.onNodeWithText("Skip setup?").assertIsDisplayed()
    composeTestRule
        .onNodeWithText(
            composeTestRule.activity.getString(R.string.onboarding_skip_confirmation_body)
        )
        .assertIsDisplayed()
    composeTestRule.runOnIdle { assertFalse(finished) }
    composeTestRule.onNodeWithText("Skip for now").performClick()

    composeTestRule.runOnIdle { assertTrue(finished) }
    composeTestRule.onNodeWithText("Skip setup?").assertDoesNotExist()
  }

  @Test
  fun skip_continueSetup_dismissesConfirmationAndKeepsCurrentStep() {
    var finished = false
    setOnboarding(
        state = MainScreenState(disclosureAccepted = true),
        initialStep = OnboardingStep.Service,
        onFinish = { finished = true },
    )

    composeTestRule.onNodeWithTag(ONBOARDING_SKIP_BUTTON_TAG).performClick()
    composeTestRule.onNodeWithText("Continue setup").performClick()

    composeTestRule.onNodeWithText("Skip setup?").assertDoesNotExist()
    composeTestRule.onNodeWithText("Step 2 of 4").assertIsDisplayed()
    composeTestRule.runOnIdle { assertFalse(finished) }
  }

  @Test
  fun skip_systemBack_dismissesConfirmationAndKeepsCurrentStep() {
    var finished = false
    setOnboarding(
        state = MainScreenState(disclosureAccepted = true),
        initialStep = OnboardingStep.Service,
        onFinish = { finished = true },
    )

    composeTestRule.onNodeWithTag(ONBOARDING_SKIP_BUTTON_TAG).performClick()
    composeTestRule.onNodeWithText("Skip setup?").assertIsDisplayed()
    pressBack()

    composeTestRule.onNodeWithText("Skip setup?").assertDoesNotExist()
    composeTestRule.onNodeWithText("Step 2 of 4").assertIsDisplayed()
    composeTestRule.runOnIdle { assertFalse(finished) }
  }

  @Test
  fun skipConfirmation_survivesStateRestoration_andFinishesOnlyOnceConfirmed() {
    val restorationTester = StateRestorationTester(composeTestRule)
    var finishCount = 0
    setOnboarding(
        state = MainScreenState(disclosureAccepted = true),
        initialStep = OnboardingStep.Service,
        onFinish = { finishCount++ },
        restorationTester = restorationTester,
    )
    composeTestRule.onNodeWithTag(ONBOARDING_SKIP_BUTTON_TAG).performClick()

    restorationTester.emulateSavedInstanceStateRestore()

    composeTestRule.onNodeWithText("Skip setup?").assertIsDisplayed()
    composeTestRule.runOnIdle { assertEquals(0, finishCount) }
    composeTestRule.onNodeWithText("Continue setup").performClick()
    composeTestRule.onNodeWithText("Step 2 of 4").assertIsDisplayed()
    composeTestRule.onNodeWithTag(ONBOARDING_SKIP_BUTTON_TAG).performClick()
    composeTestRule.onNodeWithText("Skip for now").performClick()
    composeTestRule.runOnIdle { assertEquals(1, finishCount) }
  }

  private fun allSet() =
      MainScreenState(
          disclosureAccepted = true,
          serviceEnabled = true,
          selectedAppState = SelectedAppState.Valid(app),
          backgroundProtection = BackgroundProtectionState(batteryOptimizationIgnored = true),
      )

  private fun setOnboarding(
      state: MainScreenState,
      initialStep: OnboardingStep,
      onAcceptDisclosure: () -> Unit = {},
      onOpenAccessibilitySettings: () -> Unit = {},
      onStepChanged: (OnboardingStep) -> Unit = {},
      onFinish: () -> Unit = {},
      restorationTester: StateRestorationTester? = null,
      serviceMenuHint: ServiceMenuHint = ServiceMenuHint.Other,
  ) {
    val content: @Composable () -> Unit = {
      A11YButtonTheme {
        var step by rememberSaveable { mutableStateOf(initialStep) }
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
            serviceMenuHint = serviceMenuHint,
        )
      }
    }
    if (restorationTester != null) restorationTester.setContent(content)
    else composeTestRule.setContent(content)
  }
}
