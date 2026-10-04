/*
 * Copyright (C) 2026 Pedro Veloso
 * SPDX-License-Identifier: Apache-2.0
 */
package com.pedronveloso.a11ybutton

import android.app.Application
import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.pedronveloso.a11ybutton.ui.MainViewModel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Covers the false return of [MainViewModel.selectCreatedShortcut], which drives the error UI. Only
 * unusable results are exercised: a usable one would overwrite the device's saved selection.
 */
@RunWith(AndroidJUnit4::class)
class MainViewModelShortcutTest {
  private lateinit var viewModel: MainViewModel

  @Before
  fun createViewModel() {
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    val application = instrumentation.targetContext.applicationContext as Application
    instrumentation.runOnMainSync { viewModel = MainViewModel(application) }
  }

  private fun select(result: Intent?) = runBlocking {
    viewModel.selectCreatedShortcut(packageName = PACKAGE, appLabel = "Mail", result = result)
  }

  @Test
  fun selectCreatedShortcut_returnsFalse_whenTheAppReturnedNoResult() {
    assertFalse(select(null))
  }

  @Test
  fun selectCreatedShortcut_returnsFalse_whenTheResultHasNoShortcutIntent() {
    assertFalse(select(Intent().putExtra(Intent.EXTRA_SHORTCUT_NAME, "Compose")))
  }

  @Test
  fun selectCreatedShortcut_returnsFalse_whenTheShortcutTargetsAnotherPackage() {
    val foreign = Intent("a.b.ACTION").setPackage("com.someone.else")

    assertFalse(select(Intent().putExtra(Intent.EXTRA_SHORTCUT_INTENT, foreign)))
  }

  private companion object {
    const val PACKAGE = "com.example.mail"
  }
}
