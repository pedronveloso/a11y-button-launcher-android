/*
 * Copyright (C) 2026 Pedro Veloso
 * SPDX-License-Identifier: Apache-2.0
 */
package com.pedronveloso.a11ybutton.data

import com.pedronveloso.a11ybutton.model.InstalledApp
import com.pedronveloso.a11ybutton.model.ShortcutEntry
import com.pedronveloso.a11ybutton.model.ShortcutTarget
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ShortcutsRepositoryTest {
  private val mail = app("com.example.mail", "Mail")
  private val calendar = app("com.example.calendar", "calendar")
  private val zoo = app("com.example.zoo", "Zoo")

  @Test
  fun buildShortcutGroups_ordersAppsAlphabeticallyIgnoringCase() {
    val groups =
        buildShortcutGroups(
            listOf(
                zoo to ready(zoo, "Feed"),
                mail to ready(mail, "Compose"),
                calendar to ready(calendar, "New event"),
            ),
        )

    assertEquals(listOf("calendar", "Mail", "Zoo"), groups.map { it.app.label })
  }

  @Test
  fun buildShortcutGroups_groupsByAppAndOrdersShortcutsAlphabetically() {
    val groups =
        buildShortcutGroups(
            listOf(
                mail to ready(mail, "search"),
                mail to ready(mail, "Compose"),
                mail to ready(mail, "Inbox"),
            ),
        )

    assertEquals(1, groups.size)
    assertEquals(listOf("Compose", "Inbox", "search"), groups.single().entries.map { it.label })
  }

  @Test
  fun buildShortcutGroups_returnsNothing_whenThereAreNoEntries() {
    assertTrue(buildShortcutGroups(emptyList()).isEmpty())
  }

  @Test
  fun filterByQuery_returnsEverything_whenQueryIsBlank() {
    val groups = sampleGroups()

    assertEquals(groups, groups.filterByQuery("  "))
  }

  @Test
  fun filterByQuery_matchesShortcutLabelIgnoringCase() {
    val result = sampleGroups().filterByQuery("COMPOSE")

    assertEquals(listOf("Mail"), result.map { it.app.label })
    assertEquals(listOf("Compose"), result.single().entries.map { it.label })
  }

  @Test
  fun filterByQuery_keepsAllShortcuts_whenAppNameMatches() {
    val result = sampleGroups().filterByQuery("mAiL")

    assertEquals(listOf("Compose", "Inbox"), result.single().entries.map { it.label })
  }

  @Test
  fun filterByQuery_dropsAppsWithNoMatches() {
    assertTrue(sampleGroups().filterByQuery("nothing like this").isEmpty())
  }

  private fun sampleGroups() =
      buildShortcutGroups(
          listOf(
              mail to ready(mail, "Compose"),
              mail to ready(mail, "Inbox"),
              calendar to ready(calendar, "New event"),
          ),
      )

  private fun app(packageName: String, label: String) =
      InstalledApp(packageName = packageName, componentName = "$packageName/.Main", label = label)

  private fun ready(app: InstalledApp, label: String): ShortcutEntry =
      ShortcutEntry.Ready(
          ShortcutTarget(
              packageName = app.packageName,
              shortcutId = label,
              label = label,
              intentUri = "intent:#Intent;package=${app.packageName};S.l=$label;end",
          ),
      )
}
