/*
 * Copyright (C) 2026 Pedro Veloso
 * SPDX-License-Identifier: Apache-2.0
 */
package com.pedronveloso.a11ybutton

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LogViewerNavigationTest {
  @get:Rule val rule = createAndroidComposeRule<MainActivity>()

  @Test
  fun viewer_returnsToSettingsAfterRecreation() {
    enableLogging()
    rule.onNodeWithContentDescription("Preferences").performClick()
    rule.onNodeWithText("View logs").performScrollTo().performClick()
    rule.onNodeWithTag("logs_tab").assertIsDisplayed()
    rule.activityRule.scenario.recreate()
    rule.onNodeWithTag("logs_tab").assertIsDisplayed()
    rule.onNodeWithContentDescription("Go back").performClick()
    rule.onNodeWithText("Save logs on this device").performScrollTo().assertIsDisplayed()
  }

  @Test
  fun viewer_returnsToDebugMenuAfterRecreation() {
    enableLogging()
    rule.onNodeWithContentDescription("Open debug menu").performClick()
    rule.onNodeWithText("View logs").performClick()
    rule.onNodeWithTag("logs_tab").assertIsDisplayed()
    rule.activityRule.scenario.recreate()
    rule.onNodeWithContentDescription("Go back").performClick()
    rule.onNodeWithText("Service diagnostics").assertIsDisplayed()
  }

  @Test
  fun viewer_explainsWhenLoggingTurnsOffWhileOpen() {
    enableLogging()
    rule.onNodeWithContentDescription("Preferences").performClick()
    rule.onNodeWithText("View logs").performScrollTo().performClick()
    rule.onNodeWithTag("logs_tab").assertIsDisplayed()
    val controller = (rule.activity.application as A11YButtonApplication).loggingController
    controller.setEnabled(false)
    rule.waitUntil(10_000) {
      !controller.state.value.enabled && !controller.state.value.transitioning
    }
    rule.onNodeWithText("Turn on in-app logging in Settings to view logs.").assertIsDisplayed()
    rule.onNodeWithContentDescription("Back").performClick()
    rule.onNodeWithText("Save logs on this device").performScrollTo().assertIsDisplayed()
  }

  private fun enableLogging() {
    val controller = (rule.activity.application as A11YButtonApplication).loggingController
    rule.waitUntil(10_000) { !controller.state.value.transitioning }
    controller.setEnabled(true)
    rule.waitUntil(10_000) {
      controller.state.value.enabled && !controller.state.value.transitioning
    }
  }
}
