/*
 * Copyright (C) 2026 Pedro Veloso
 * SPDX-License-Identifier: Apache-2.0
 */
package com.pedronveloso.a11ybutton

import android.content.ComponentName
import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.pedronveloso.a11ybutton.data.InstalledAppsRepository
import com.pedronveloso.a11ybutton.model.AppSettings
import com.pedronveloso.a11ybutton.model.InvalidSelectionReason
import com.pedronveloso.a11ybutton.model.SelectedAppState
import com.pedronveloso.a11ybutton.model.ShortcutTarget
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/** Maps saved shortcuts to a [SelectedAppState]. Reads only; it never touches saved settings. */
@RunWith(AndroidJUnit4::class)
class InstalledAppsRepositoryShortcutTest {
  private val context = InstrumentationRegistry.getInstrumentation().targetContext
  private val repository = InstalledAppsRepository(context)

  private fun validate(shortcut: ShortcutTarget) =
      repository.validateSelection(AppSettings(selectedShortcut = shortcut))

  private fun shortcut(packageName: String, intentUri: String) =
      ShortcutTarget(
          packageName = packageName,
          shortcutId = "id",
          label = "Label",
          intentUri = intentUri,
      )

  private fun explicitUri(className: String) =
      Intent(Intent.ACTION_MAIN)
          .setComponent(ComponentName(context.packageName, className))
          .toUri(Intent.URI_INTENT_SCHEME)

  @Test
  fun validateSelection_isMissingApp_whenTheShortcutPackageIsNotInstalled() {
    val missing = "com.example.not.installed"
    val result = validate(shortcut(missing, "intent:#Intent;package=$missing;end"))

    assertEquals(
        SelectedAppState.Invalid(missing, null, InvalidSelectionReason.MissingApp),
        result,
    )
  }

  @Test
  fun validateSelection_isNotResolvable_whenTheActivityDoesNotExist() {
    val result = validate(shortcut(context.packageName, explicitUri("${context.packageName}.Gone")))

    assertEquals(
        SelectedAppState.Invalid(
            context.packageName,
            null,
            InvalidSelectionReason.ShortcutNotResolvable,
        ),
        result,
    )
  }

  @Test
  fun validateSelection_isNotResolvable_whenTheSavedUriCannotBeParsed() {
    val result =
        validate(
            shortcut(
                context.packageName,
                "intent:#Intent;package=${context.packageName};launchFlags=zzz;end",
            ),
        )

    assertEquals(
        SelectedAppState.Invalid(
            context.packageName,
            null,
            InvalidSelectionReason.ShortcutNotResolvable,
        ),
        result,
    )
  }

  @Test
  fun validateSelection_isValidShortcut_whenTheActivityIsExportedAndEnabled() {
    val target = shortcut(context.packageName, explicitUri(MainActivity::class.java.name))

    assertEquals(SelectedAppState.ValidShortcut(target), validate(target))
  }
}
