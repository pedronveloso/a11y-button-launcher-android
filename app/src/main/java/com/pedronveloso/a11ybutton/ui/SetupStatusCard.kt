/*
 * Copyright (C) 2026 Pedro Veloso
 * SPDX-License-Identifier: Apache-2.0
 */
package com.pedronveloso.a11ybutton.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.pedronveloso.a11ybutton.R
import com.pedronveloso.a11ybutton.model.SelectedAppState
import com.pedronveloso.a11ybutton.ui.theme.a11YButtonStatusPalette

/**
 * The Home status card: one row per setup requirement. Rows that are fine stay compact (title
 * only); rows that need attention grow to carry a description and open the place that fixes them.
 */
@Composable
fun SetupStatusCard(
    screenState: MainScreenState,
    onItemClick: (SetupItem) -> Unit,
    modifier: Modifier = Modifier,
) {
  val items = screenState.setupStatusItems()
  val remaining = items.count { !it.isDone }
  Card(modifier = modifier.fillMaxWidth()) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(16.dp),
    ) {
      Text(
          text =
              stringResource(
                  id =
                      if (screenState.isReady) {
                        R.string.home_ready_title
                      } else {
                        R.string.home_attention_title
                      },
              ),
          style = MaterialTheme.typography.titleLarge,
          modifier = Modifier.semantics { heading() },
      )
      if (!screenState.isReady && remaining > 0) {
        Text(
            text = pluralStringResource(R.plurals.home_steps_left, remaining, remaining),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      items.forEach { entry ->
        SetupStatusRow(
            entry = entry,
            invalidSelection = screenState.selectedAppState is SelectedAppState.Invalid,
            onClick = { onItemClick(entry.item) },
        )
      }
    }
  }
}

@Composable
private fun SetupStatusRow(
    entry: SetupStatusItem,
    invalidSelection: Boolean,
    onClick: () -> Unit,
) {
  val palette = a11YButtonStatusPalette()
  val shape = RoundedCornerShape(12.dp)
  val title = stringResource(setupItemTitle(entry.item))
  if (entry.isDone) {
    val doneDescription = stringResource(R.string.status_state_done)
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            Modifier.fillMaxWidth()
                .clip(shape)
                .background(palette.positiveContainer)
                .clickable(role = Role.Button, onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .semantics(mergeDescendants = true) { stateDescription = doneDescription },
    ) {
      Icon(
          imageVector = Icons.Filled.CheckCircle,
          contentDescription = null,
          tint = palette.positiveContent,
          modifier = Modifier.size(20.dp),
      )
      Text(
          text = title,
          style = MaterialTheme.typography.titleSmall,
          color = palette.positiveContent,
      )
    }
  } else {
    val attentionDescription = stringResource(R.string.status_state_needs_attention)
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            Modifier.fillMaxWidth()
                .clip(shape)
                .background(palette.warningContainer)
                .clickable(role = Role.Button, onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 16.dp)
                .semantics(mergeDescendants = true) { stateDescription = attentionDescription },
    ) {
      Icon(
          imageVector = Icons.Filled.Warning,
          contentDescription = null,
          tint = palette.warningContent,
          modifier = Modifier.size(24.dp),
      )
      Column(
          verticalArrangement = Arrangement.spacedBy(2.dp),
          modifier = Modifier.weight(1f),
      ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = palette.warningContent,
        )
        Text(
            text = stringResource(setupItemDescription(entry.item, invalidSelection)),
            style = MaterialTheme.typography.bodyMedium,
            color = palette.warningContent,
        )
      }
      Icon(
          imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
          contentDescription = null,
          tint = palette.warningContent,
      )
    }
  }
}

private fun setupItemTitle(item: SetupItem): Int =
    when (item) {
      SetupItem.Disclosure -> R.string.status_disclosure_title
      SetupItem.Service -> R.string.status_service_title
      SetupItem.ButtonAction -> R.string.status_action_title
      SetupItem.Battery -> R.string.status_battery_title
      SetupItem.RecentsLock -> R.string.status_recents_title
    }

private fun setupItemDescription(
    item: SetupItem,
    invalidSelection: Boolean,
): Int =
    when (item) {
      SetupItem.Disclosure -> R.string.status_disclosure_description
      SetupItem.Service -> R.string.status_service_description
      SetupItem.ButtonAction ->
          if (invalidSelection) {
            R.string.status_action_invalid_description
          } else {
            R.string.status_action_description
          }
      SetupItem.Battery -> R.string.status_battery_description
      SetupItem.RecentsLock -> R.string.status_recents_description
    }
