/*
 * Copyright (C) 2026 Pedro Veloso
 * SPDX-License-Identifier: Apache-2.0
 */
package com.pedronveloso.a11ybutton

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pedronveloso.a11ybutton.model.InstalledApp
import com.pedronveloso.a11ybutton.model.InvalidSelectionReason
import com.pedronveloso.a11ybutton.model.SelectedAppState
import com.pedronveloso.a11ybutton.model.ShortcutTarget
import com.pedronveloso.a11ybutton.ui.BackgroundProtectionState
import com.pedronveloso.a11ybutton.ui.MainScreenState
import com.pedronveloso.a11ybutton.ui.SetupReadiness
import com.pedronveloso.a11ybutton.ui.theme.A11YButtonTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private val BatteryAllowed = BackgroundProtectionState(batteryOptimizationIgnored = true)

@RunWith(AndroidJUnit4::class)
class HomeScreenTest {
  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  @Test
  fun homeScreen_showsReadyStateAndSelectedAppDetails() {
    composeTestRule.setContent {
      A11YButtonTheme {
        HomeScreen(
            screenState =
                MainScreenState(
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
                    backgroundProtection = BatteryAllowed,
                    readiness = SetupReadiness.Ready,
                ),
            onOpenSetup = {},
            onChooseApp = {},
            onOpenFaq = {},
            onDismissServiceMessage = {},
            onEnableNotifications = {},
        )
      }
    }

    composeTestRule.onNodeWithText("Shortcut ready").assertIsDisplayed()
    composeTestRule.onNodeWithText("Change what opens").assertIsDisplayed()
    composeTestRule.onNodeWithText("Opens an app").assertIsDisplayed()
    composeTestRule.onNodeWithText("Reader").assertIsDisplayed()
    composeTestRule.onNodeWithText("com.example.reader").assertIsDisplayed()
    composeTestRule.onNodeWithText("Open FAQ").performScrollTo().assertIsDisplayed()
  }

  @Test
  fun homeScreen_labelsSelectedTargetAsAppShortcut() {
    composeTestRule.setContent {
      A11YButtonTheme {
        HomeScreen(
            screenState =
                MainScreenState(
                    serviceEnabled = true,
                    disclosureAccepted = true,
                    selectedAppState =
                        SelectedAppState.ValidShortcut(
                            shortcut =
                                ShortcutTarget(
                                    packageName = "com.example.mail",
                                    shortcutId = "compose",
                                    label = "Compose",
                                    intentUri = "intent:#Intent;package=com.example.mail;end",
                                ),
                        ),
                    backgroundProtection = BatteryAllowed,
                    readiness = SetupReadiness.Ready,
                ),
            onOpenSetup = {},
            onChooseApp = {},
            onOpenFaq = {},
            onDismissServiceMessage = {},
            onEnableNotifications = {},
        )
      }
    }

    composeTestRule.onNodeWithText("Opens an app shortcut").assertIsDisplayed()
    composeTestRule.onNodeWithText("Compose").assertIsDisplayed()
    composeTestRule.onNodeWithText("com.example.mail").assertIsDisplayed()
    composeTestRule.onNodeWithText("Change what opens").assertIsDisplayed()
  }

  @Test
  fun homeScreen_keepsSetupEntryVisible_whenReady() {
    var setupOpened = false

    composeTestRule.setContent {
      A11YButtonTheme {
        HomeScreen(
            screenState =
                MainScreenState(
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
                    backgroundProtection = BatteryAllowed,
                    readiness = SetupReadiness.Ready,
                ),
            onOpenSetup = { setupOpened = true },
            onChooseApp = {},
            onOpenFaq = {},
            onDismissServiceMessage = {},
            onEnableNotifications = {},
        )
      }
    }

    composeTestRule
        .onNodeWithTag(HOME_STATUS_OPEN_SETUP_BUTTON_TAG)
        .performScrollTo()
        .assertIsDisplayed()
    composeTestRule.onNodeWithText("Setup & troubleshooting").performClick()

    composeTestRule.runOnIdle { assertTrue(setupOpened) }
  }

  @Test
  fun homeScreen_invokesSetupAndDismissCallbacks_whenNotReady() {
    var setupOpened = false
    var messageDismissed = false

    composeTestRule.setContent {
      A11YButtonTheme {
        HomeScreen(
            screenState =
                MainScreenState(
                    serviceEnabled = false,
                    disclosureAccepted = false,
                    selectedAppState = SelectedAppState.None,
                    serviceMessage = "Choose an app before using the Accessibility shortcut.",
                    readiness = SetupReadiness.NotSetUp,
                ),
            onOpenSetup = { setupOpened = true },
            onChooseApp = {},
            onOpenFaq = {},
            onDismissServiceMessage = { messageDismissed = true },
            onEnableNotifications = {},
        )
      }
    }

    composeTestRule.onNodeWithText("Dismiss").performClick()
    composeTestRule
        .onNodeWithTag(HOME_STATUS_OPEN_SETUP_BUTTON_TAG)
        .performScrollTo()
        .performClick()

    composeTestRule.runOnIdle {
      assertTrue(setupOpened)
      assertTrue(messageDismissed)
    }
  }

  @Test
  fun homeScreen_showsInvalidSelectionGuidance() {
    composeTestRule.setContent {
      A11YButtonTheme {
        HomeScreen(
            screenState =
                MainScreenState(
                    serviceEnabled = true,
                    disclosureAccepted = true,
                    selectedAppState =
                        SelectedAppState.Invalid(
                            packageName = "com.example.reader",
                            componentName = "com.example.reader/.HomeActivity",
                            reason = InvalidSelectionReason.MissingApp,
                        ),
                    readiness = SetupReadiness.PartiallySetUp,
                ),
            onOpenSetup = {},
            onChooseApp = {},
            onOpenFaq = {},
            onDismissServiceMessage = {},
            onEnableNotifications = {},
        )
      }
    }

    composeTestRule.onNodeWithText("Setup needed").assertIsDisplayed()
    composeTestRule.onNodeWithText("Saved selection needs attention").assertIsDisplayed()
    composeTestRule.onNodeWithText("The selected app is no longer installed.").assertIsDisplayed()
    composeTestRule.onNodeWithText("Package: com.example.reader").assertIsDisplayed()
    composeTestRule.onNodeWithText("Change what opens").assertIsDisplayed()
  }

  @Test
  fun homeScreen_callsChooseApp_whenActionButtonIsPressed() {
    var chooseAppCount = 0

    composeTestRule.setContent {
      A11YButtonTheme {
        HomeScreen(
            screenState =
                MainScreenState(
                    serviceEnabled = false,
                    disclosureAccepted = false,
                    selectedAppState = SelectedAppState.None,
                    readiness = SetupReadiness.NotSetUp,
                ),
            onOpenSetup = {},
            onChooseApp = { chooseAppCount += 1 },
            onOpenFaq = {},
            onDismissServiceMessage = {},
            onEnableNotifications = {},
        )
      }
    }

    composeTestRule.onNodeWithText("Choose what to open").performClick()

    composeTestRule.runOnIdle { assertEquals(1, chooseAppCount) }
  }

  @Test
  fun statusCard_showsDescriptionsOnlyForRowsThatNeedAttention() {
    composeTestRule.setContent {
      A11YButtonTheme {
        HomeScreen(
            screenState =
                MainScreenState(
                    serviceEnabled = true,
                    disclosureAccepted = true,
                    selectedAppState = SelectedAppState.None,
                    readiness = SetupReadiness.PartiallySetUp,
                ),
            onOpenSetup = {},
            onChooseApp = {},
            onOpenFaq = {},
            onDismissServiceMessage = {},
            onEnableNotifications = {},
        )
      }
    }

    composeTestRule.onNodeWithText("2 steps left").assertIsDisplayed()
    // Done rows are a title only.
    composeTestRule.onNodeWithText("Accessibility service").assertIsDisplayed()
    composeTestRule
        .onNodeWithText("Turn it on in Accessibility settings so the button can work.")
        .assertDoesNotExist()
    // Rows needing attention carry a description.
    composeTestRule.onNodeWithText("What the button opens").assertIsDisplayed()
    composeTestRule
        .onNodeWithText("Choose the app or shortcut the button opens.")
        .assertIsDisplayed()
    composeTestRule.onNodeWithText("Unrestricted battery").assertIsDisplayed()
    composeTestRule
        .onNodeWithText("Stops your phone from putting the service to sleep.")
        .assertIsDisplayed()
  }

  @Test
  fun statusCard_routesAttentionRowsToTheirFix() {
    var setupOpened = 0
    var chooseAppCount = 0

    composeTestRule.setContent {
      A11YButtonTheme {
        HomeScreen(
            screenState =
                MainScreenState(
                    serviceEnabled = true,
                    disclosureAccepted = true,
                    selectedAppState = SelectedAppState.None,
                    readiness = SetupReadiness.PartiallySetUp,
                ),
            onOpenSetup = { setupOpened += 1 },
            onChooseApp = { chooseAppCount += 1 },
            onOpenFaq = {},
            onDismissServiceMessage = {},
            onEnableNotifications = {},
        )
      }
    }

    composeTestRule.onNodeWithText("What the button opens").performClick()
    composeTestRule.onNodeWithText("Unrestricted battery").performClick()

    composeTestRule.runOnIdle {
      assertEquals(1, chooseAppCount)
      assertEquals(1, setupOpened)
    }
  }

  @Test
  fun statusCard_hasNoStepsLeftLine_whenReady() {
    composeTestRule.setContent {
      A11YButtonTheme {
        HomeScreen(
            screenState =
                MainScreenState(
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
                    backgroundProtection = BatteryAllowed,
                    readiness = SetupReadiness.Ready,
                ),
            onOpenSetup = {},
            onChooseApp = {},
            onOpenFaq = {},
            onDismissServiceMessage = {},
            onEnableNotifications = {},
        )
      }
    }

    composeTestRule.onNodeWithText("Shortcut ready").assertIsDisplayed()
    composeTestRule.onNodeWithText("Unrestricted battery").assertIsDisplayed()
    composeTestRule
        .onNodeWithText("Stops your phone from putting the service to sleep.")
        .assertDoesNotExist()
  }
}
