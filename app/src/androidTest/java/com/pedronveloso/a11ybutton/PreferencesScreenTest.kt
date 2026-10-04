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
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
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
}
