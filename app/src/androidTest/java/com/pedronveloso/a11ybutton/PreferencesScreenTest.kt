/*
 * Copyright (C) 2026 Pedro Veloso
 * SPDX-License-Identifier: Apache-2.0
 */
package com.pedronveloso.a11ybutton

import androidx.activity.ComponentActivity
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pedronveloso.a11ybutton.logging.LoggingState
import com.pedronveloso.a11ybutton.model.ThemeMode
import com.pedronveloso.a11ybutton.ui.theme.A11YButtonTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PreferencesScreenTest {
  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  @Test
  fun preferencesScreen_keepsFaqReachable() {
    var faqOpened = 0
    composeTestRule.setContent {
      A11YButtonTheme {
        PreferencesScreen(
            themeMode = ThemeMode.SYSTEM,
            onThemeModeChanged = {},
            onOpenFaq = { faqOpened++ },
        )
      }
    }

    composeTestRule.onNodeWithText("Open FAQ").performScrollTo().assertIsDisplayed().performClick()

    assertEquals(1, faqOpened)
  }

  @Test
  fun loggingSwitch_exposesStateAndControlsViewerAvailability() {
    val state = mutableStateOf(LoggingState(transitioning = false))
    var viewerOpened = 0
    composeTestRule.setContent {
      A11YButtonTheme {
        PreferencesScreen(
            themeMode = ThemeMode.SYSTEM,
            onThemeModeChanged = {},
            onOpenFaq = {},
            loggingState = state.value,
            onLoggingChanged = { state.value = LoggingState(enabled = it, transitioning = false) },
            onOpenLogs = { viewerOpened++ },
        )
      }
    }
    val switch = composeTestRule.onNodeWithText("Save logs on this device")
    switch.performScrollTo().assertIsOff()
    composeTestRule.onNodeWithText("View logs").performScrollTo().assertIsNotEnabled()
    switch.performScrollTo().performClick().assertIsOn()
    composeTestRule.onNodeWithText("View logs").performScrollTo().performClick()
    assertEquals(1, viewerOpened)
    composeTestRule.runOnIdle { state.value = LoggingState(enabled = true, transitioning = true) }
    switch.performScrollTo().assertIsNotEnabled()
    composeTestRule.onNodeWithText("View logs").performScrollTo().assertIsNotEnabled()
  }
}
