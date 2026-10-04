/*
 * Copyright (C) 2026 Pedro Veloso
 * SPDX-License-Identifier: Apache-2.0
 */
package com.pedronveloso.a11ybutton.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pedronveloso.a11ybutton.AppIcon
import com.pedronveloso.a11ybutton.R
import com.pedronveloso.a11ybutton.model.AppPickerShortcuts
import com.pedronveloso.a11ybutton.model.ShortcutEntry
import com.pedronveloso.a11ybutton.model.ShortcutTarget

/** App shortcuts grouped under a sticky header per app. */
@Composable
internal fun ShortcutPickerList(
    shortcuts: AppPickerShortcuts,
    selectedShortcut: ShortcutTarget?,
    query: String,
    onShortcutSelected: (ShortcutTarget) -> Unit,
    onCreateShortcut: (ShortcutEntry.Creator, String) -> Unit,
    modifier: Modifier = Modifier,
) {
  Box(modifier = modifier.fillMaxSize()) {
    if (shortcuts.isLoading) {
      CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
    } else if (shortcuts.groups.isEmpty()) {
      // With no search, nothing was filtered out: the device simply exposes no shortcuts.
      val emptyText =
          if (query.isBlank()) {
            R.string.picker_shortcuts_none
          } else {
            R.string.picker_shortcuts_empty
          }
      Text(
          text = stringResource(id = emptyText),
          style = MaterialTheme.typography.bodyMedium,
          modifier = Modifier.align(Alignment.Center),
      )
    } else {
      LazyColumn(
          verticalArrangement = Arrangement.spacedBy(12.dp),
          modifier = Modifier.fillMaxSize(),
      ) {
        shortcuts.groups.forEach { group ->
          stickyHeader(key = "header:${group.app.packageName}") {
            Surface(
                color = MaterialTheme.colorScheme.background,
                modifier = Modifier.fillMaxWidth(),
            ) {
              Row(
                  horizontalArrangement = Arrangement.spacedBy(12.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  modifier = Modifier.padding(vertical = 8.dp).semantics { heading() },
              ) {
                AppIcon(componentName = group.app.componentName, contentDescription = null)
                Text(
                    text = group.app.label,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
              }
            }
          }
          items(
              items = group.entries,
              key = { entry -> "${group.app.packageName}|${entry.stableKey()}" },
          ) { entry ->
            when (entry) {
              is ShortcutEntry.Ready ->
                  ShortcutRow(
                      title = entry.label,
                      supportingText = null,
                      isSelected = selectedShortcut?.isSameShortcutAs(entry.target) == true,
                      onClick = { onShortcutSelected(entry.target) },
                  )
              is ShortcutEntry.Creator ->
                  ShortcutRow(
                      title = entry.label,
                      supportingText = stringResource(id = R.string.picker_shortcut_create_hint),
                      isSelected = false,
                      onClick = { onCreateShortcut(entry, group.app.label) },
                  )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun ShortcutRow(
    title: String,
    supportingText: String?,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
  val stateDescription =
      if (isSelected) {
        stringResource(id = R.string.picker_state_selected, title)
      } else {
        stringResource(id = R.string.picker_state_not_selected)
      }
  Card(
      colors =
          CardDefaults.cardColors(
              containerColor =
                  if (isSelected) {
                    MaterialTheme.colorScheme.secondaryContainer
                  } else {
                    MaterialTheme.colorScheme.surface
                  },
          ),
      modifier =
          Modifier.fillMaxWidth()
              .selectable(selected = isSelected, onClick = onClick, role = Role.Button)
              .semantics {
                selected = isSelected
                this.stateDescription = stateDescription
              },
  ) {
    Column(
        verticalArrangement = Arrangement.spacedBy(2.dp),
        modifier = Modifier.padding(16.dp),
    ) {
      Text(text = title, style = MaterialTheme.typography.titleMedium)
      supportingText?.let { Text(text = it, style = MaterialTheme.typography.bodySmall) }
      if (isSelected) {
        Text(
            text = stringResource(id = R.string.picker_selected_badge),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
        )
      }
    }
  }
}

private fun ShortcutEntry.stableKey(): String =
    when (this) {
      is ShortcutEntry.Ready -> "${target.shortcutId.orEmpty()}|${target.intentUri}"
      is ShortcutEntry.Creator -> componentName
    }
