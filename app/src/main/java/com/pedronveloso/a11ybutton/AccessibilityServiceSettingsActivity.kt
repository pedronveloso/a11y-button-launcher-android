/*
 * Copyright (C) 2026 Pedro Veloso
 * SPDX-License-Identifier: Apache-2.0
 */
package com.pedronveloso.a11ybutton

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.pedronveloso.a11ybutton.ui.theme.A11YButtonTheme
import timber.log.Timber

class AccessibilityServiceSettingsActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    Timber.i("AccessibilityServiceSettingsActivity created")
    enableEdgeToEdge()
    setContent { A11YButtonTheme { AccessibilityServiceSettingsRoute(onBack = ::finish) } }
  }
}

@Composable
private fun AccessibilityServiceSettingsRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
  val context = LocalContext.current

  AccessibilityServiceSettingsScreen(
      onBack = onBack,
      onOpenApp = { SystemSettingsNavigator.openApp(context) },
      onOpenAppDetails = { SystemSettingsNavigator.openAppDetails(context) },
      modifier = modifier,
  )
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun AccessibilityServiceSettingsScreen(
    onBack: () -> Unit,
    onOpenApp: () -> Unit,
    onOpenAppDetails: () -> Unit,
    modifier: Modifier = Modifier,
) {
  Scaffold(
      modifier = modifier.fillMaxSize(),
      topBar = {
        TopAppBar(
            title = { Text(text = stringResource(id = R.string.service_settings_title)) },
            navigationIcon = {
              IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(id = R.string.picker_back),
                )
              }
            },
        )
      },
  ) { innerPadding ->
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier =
            Modifier.fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
    ) {
      Text(
          text = stringResource(id = R.string.service_settings_body),
          style = MaterialTheme.typography.bodyLarge,
      )

      SettingsSectionCard(title = stringResource(id = R.string.service_settings_actions_title)) {
        Button(
            onClick = onOpenApp,
            modifier = Modifier.fillMaxWidth(),
        ) {
          Text(text = stringResource(id = R.string.service_settings_open_app))
        }
        OutlinedButton(
            onClick = onOpenAppDetails,
            modifier = Modifier.fillMaxWidth(),
        ) {
          Text(text = stringResource(id = R.string.service_settings_open_app_info))
        }
      }
    }
  }
}

@Composable
private fun SettingsSectionCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
  Card(modifier = modifier.fillMaxWidth()) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.padding(16.dp),
    ) {
      Text(
          text = title,
          style = MaterialTheme.typography.titleMedium,
          modifier = Modifier.semantics { heading() },
      )
      content()
    }
  }
}
