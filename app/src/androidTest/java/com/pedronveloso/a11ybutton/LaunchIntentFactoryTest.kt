/*
 * Copyright (C) 2026 Pedro Veloso
 * SPDX-License-Identifier: Apache-2.0
 */
package com.pedronveloso.a11ybutton

import android.content.Intent
import android.os.Bundle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pedronveloso.a11ybutton.service.LaunchIntentFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LaunchIntentFactoryTest {
  private fun uri(intent: Intent) = intent.toUri(Intent.URI_INTENT_SCHEME)

  @Test
  fun createShortcutIntent_pinsTheOwningPackage_whenTheIntentIsImplicit() {
    val intent = LaunchIntentFactory.createShortcutIntent(uri(Intent("a.b.ACTION")), PACKAGE)

    assertNotNull(intent)
    assertEquals(PACKAGE, intent!!.`package`)
  }

  @Test
  fun createShortcutIntent_doesNotThrow_whenAnImplicitIntentCarriesASelector() {
    val withSelector = Intent("a.b.ACTION").apply { selector = Intent("a.b.OTHER") }

    val intent = LaunchIntentFactory.createShortcutIntent(uri(withSelector), PACKAGE)

    assertNotNull(intent)
    assertNull(intent!!.selector)
    assertEquals(PACKAGE, intent.`package`)
  }

  @Test
  fun createShortcutIntent_rejectsAnIntentForAnotherPackage() {
    val foreign = Intent("a.b.ACTION").setPackage("com.someone.else")

    assertNull(LaunchIntentFactory.createShortcutIntent(uri(foreign), PACKAGE))
  }

  @Test
  fun createShortcutIntent_stripsUriGrantFlags_andStartsInANewTask() {
    val flagged =
        Intent("a.b.ACTION")
            .setPackage(PACKAGE)
            .addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )

    val intent = LaunchIntentFactory.createShortcutIntent(uri(flagged), PACKAGE)!!

    assertEquals(0, intent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION)
    assertEquals(0, intent.flags and Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
    assertTrue(intent.flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0)
  }

  @Test
  fun createShortcutIntent_returnsNull_whenTheUriThrowsAnUncheckedException() {
    // parseUri throws NumberFormatException, not URISyntaxException, for a bad launchFlags value.
    val malformed = "intent:#Intent;package=$PACKAGE;launchFlags=zzz;end"

    assertNull(LaunchIntentFactory.createShortcutIntent(malformed, PACKAGE))
  }

  @Test
  fun hasOnlyUriSafeExtras_isTrue_forScalarExtras() {
    val intent =
        Intent()
            .putExtra("s", "text")
            .putExtra("b", true)
            .putExtra("i", 3)
            .putExtra("l", 4L)
            .putExtra("d", 1.5)

    assertTrue(LaunchIntentFactory.hasOnlyUriSafeExtras(intent))
  }

  @Test
  fun hasOnlyUriSafeExtras_isTrue_whenThereAreNoExtras() {
    assertTrue(LaunchIntentFactory.hasOnlyUriSafeExtras(Intent()))
  }

  @Test
  fun hasOnlyUriSafeExtras_isFalse_forBundleAndArrayExtras() {
    assertFalse(LaunchIntentFactory.hasOnlyUriSafeExtras(Intent().putExtra("b", Bundle())))
    assertFalse(
        LaunchIntentFactory.hasOnlyUriSafeExtras(Intent().putExtra("a", intArrayOf(1, 2))),
    )
  }

  private companion object {
    const val PACKAGE = "com.example.mail"
  }
}
