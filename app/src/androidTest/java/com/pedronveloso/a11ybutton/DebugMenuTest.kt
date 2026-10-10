/*
 * Copyright (C) 2026 Pedro Veloso
 * SPDX-License-Identifier: Apache-2.0
 */
package com.pedronveloso.a11ybutton

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DebugMenuTest {
  @get:Rule val rule = createAndroidComposeRule<MainActivity>()

  @Test
  fun hideDebugIcon_removesItFromHome_andKeepsItHiddenAfterRecreation() {
    rule.onNodeWithContentDescription("Open debug menu").performClick()
    rule.onNodeWithText("Hide debug icon until app restarts").performClick()

    rule.onNodeWithContentDescription("Open debug menu").assertDoesNotExist()
    rule.activityRule.scenario.recreate()
    rule.onNodeWithContentDescription("Open debug menu").assertDoesNotExist()
  }
}
