/*
 * Copyright (C) 2026 Pedro Veloso
 * SPDX-License-Identifier: Apache-2.0
 */
package com.pedronveloso.a11ybutton

import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.pedronveloso.a11ybutton.data.InstalledAppsRepository
import com.pedronveloso.a11ybutton.data.ShortcutsRepository
import com.pedronveloso.a11ybutton.model.ShortcutEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Parses a static shortcuts file that targets this app's own exported MainActivity, so the
 * launchable check passes without depending on any other installed app.
 */
@RunWith(AndroidJUnit4::class)
class ShortcutsRepositoryParsingTest {
  private lateinit var shortcuts: Map<String, Intent>

  @Before
  fun parseTestShortcuts() {
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    val targetContext = instrumentation.targetContext
    val testContext = instrumentation.context
    val repository = ShortcutsRepository(targetContext, InstalledAppsRepository(targetContext))
    val parser = testContext.resources.getXml(com.pedronveloso.a11ybutton.test.R.xml.test_shortcuts)

    shortcuts =
        repository
            .parseStaticShortcuts(parser, testContext.resources, targetContext.packageName)
            .filterIsInstance<ShortcutEntry.Ready>()
            .associate {
              it.target.shortcutId.orEmpty() to
                  Intent.parseUri(it.target.intentUri, Intent.URI_INTENT_SCHEME)
            }
  }

  @Test
  fun shortcutWithSeveralIntents_isSkipped() {
    assertNull(shortcuts["multi"])
  }

  @Test
  fun shortcutLevelCategories_areNotAddedToTheLaunchIntent() {
    val intent = shortcuts.getValue("categories")

    assertTrue(intent.hasCategory(Intent.CATEGORY_DEFAULT))
    assertTrue(!intent.hasCategory("android.shortcut.conversation"))
  }

  @Test
  fun mimeType_isKeptTogetherWithData() {
    val intent = shortcuts.getValue("typed")

    assertEquals("content://com.example/item/1", intent.dataString)
    assertEquals("vnd.example/item", intent.type)
  }

  @Test
  fun plainShortcut_isListed() {
    assertTrue(shortcuts.containsKey("plain"))
  }
}
