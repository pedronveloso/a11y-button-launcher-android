/*
 * Copyright (C) 2026 Pedro Veloso
 * SPDX-License-Identifier: Apache-2.0
 */
package com.pedronveloso.a11ybutton

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pedronveloso.a11ybutton.model.InstalledApp
import com.pedronveloso.a11ybutton.model.InvalidSelectionReason
import com.pedronveloso.a11ybutton.model.SelectedAppState
import com.pedronveloso.a11ybutton.ui.MainScreenState
import com.pedronveloso.a11ybutton.ui.SetupReadiness
import com.pedronveloso.a11ybutton.ui.theme.A11YButtonTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

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
                    readiness = SetupReadiness.Ready,
                ),
            onOpenSetup = {},
            onChooseApp = {},
            onOpenFaq = {},
            onDismissServiceMessage = {},
        )
      }
    }

    composeTestRule.onNodeWithText("Shortcut ready").assertIsDisplayed()
    composeTestRule.onNodeWithText("Change App").assertIsDisplayed()
    composeTestRule.onNodeWithText("Reader").assertIsDisplayed()
    composeTestRule.onNodeWithText("com.example.reader").assertIsDisplayed()
    composeTestRule.onNodeWithText("Open FAQ").assertIsDisplayed()
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
        )
      }
    }

    composeTestRule.onNodeWithText("Open Setup").performClick()
    composeTestRule.onNodeWithText("Dismiss").performClick()

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
        )
      }
    }

    composeTestRule.onNodeWithText("Setup needed").assertIsDisplayed()
    composeTestRule.onNodeWithText("Saved selection needs attention").assertIsDisplayed()
    composeTestRule.onNodeWithText("The selected app is no longer installed.").assertIsDisplayed()
    composeTestRule.onNodeWithText("Package: com.example.reader").assertIsDisplayed()
    composeTestRule.onNodeWithText("Change App").assertIsDisplayed()
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
        )
      }
    }

    composeTestRule.onNodeWithText("Choose App").performClick()

    composeTestRule.runOnIdle { assertEquals(1, chooseAppCount) }
  }
}
