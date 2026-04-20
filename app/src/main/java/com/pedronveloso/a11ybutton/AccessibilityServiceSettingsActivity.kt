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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pedronveloso.a11ybutton.service.ServiceDiagnostics
import com.pedronveloso.a11ybutton.service.ServiceDiagnosticsStore
import com.pedronveloso.a11ybutton.ui.theme.A11YButtonTheme
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
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
  val lifecycleOwner = LocalLifecycleOwner.current
  val diagnostics by ServiceDiagnosticsStore.state.collectAsStateWithLifecycle()
  var isIgnoringBatteryOptimizations by remember {
    mutableStateOf(SystemSettingsNavigator.isIgnoringBatteryOptimizations(context))
  }

  DisposableEffect(lifecycleOwner, context) {
    fun logRefresh() {
      Timber.d("Refreshed service settings screen diagnostics")
      isIgnoringBatteryOptimizations =
          SystemSettingsNavigator.isIgnoringBatteryOptimizations(context)
    }

    logRefresh()
    val observer = LifecycleEventObserver { _, event ->
      if (event == Lifecycle.Event.ON_RESUME) {
        logRefresh()
      }
    }
    lifecycleOwner.lifecycle.addObserver(observer)

    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
  }

  AccessibilityServiceSettingsScreen(
      diagnostics = diagnostics,
      isIgnoringBatteryOptimizations = isIgnoringBatteryOptimizations,
      onBack = onBack,
      onOpenAccessibilitySettings = { SystemSettingsNavigator.openAccessibilitySettings(context) },
      onOpenBatterySettings = { SystemSettingsNavigator.openBatteryOptimizationSettings(context) },
      onOpenAppDetails = { SystemSettingsNavigator.openAppDetails(context) },
      onOpenXiaomiHelp = { SystemSettingsNavigator.openXiaomiHelp(context) },
      modifier = modifier,
  )
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun AccessibilityServiceSettingsScreen(
    diagnostics: ServiceDiagnostics,
    isIgnoringBatteryOptimizations: Boolean,
    onBack: () -> Unit,
    onOpenAccessibilitySettings: () -> Unit,
    onOpenBatterySettings: () -> Unit,
    onOpenAppDetails: () -> Unit,
    onOpenXiaomiHelp: () -> Unit,
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
      SettingsSectionCard(
          title = stringResource(id = R.string.service_settings_diagnostics_title)
      ) {
        StatusValue(
            label = stringResource(id = R.string.service_settings_last_event_label),
            value =
                diagnostics.lastLifecycleEvent
                    ?: stringResource(id = R.string.service_settings_status_unknown),
        )
        StatusValue(
            label = stringResource(id = R.string.service_settings_last_event_at_label),
            value = formatTimestampOrUnknown(diagnostics.lastLifecycleEventAtMillis),
        )
        StatusValue(
            label = stringResource(id = R.string.service_settings_last_button_press_label),
            value = formatTimestampOrUnknown(diagnostics.lastButtonPressAtMillis),
        )
        StatusValue(
            label = stringResource(id = R.string.service_settings_last_trigger_label),
            value = formatTimestampOrUnknown(diagnostics.lastTriggerAtMillis),
        )
        StatusValue(
            label = stringResource(id = R.string.service_settings_battery_optimization_label),
            value =
                if (isIgnoringBatteryOptimizations) {
                  stringResource(id = R.string.service_settings_battery_optimization_disabled)
                } else {
                  stringResource(id = R.string.service_settings_battery_optimization_enabled)
                },
        )
      }

      SettingsSectionCard(title = stringResource(id = R.string.service_settings_actions_title)) {
        Button(
            onClick = onOpenAccessibilitySettings,
            modifier = Modifier.fillMaxWidth(),
        ) {
          Text(text = stringResource(id = R.string.service_settings_open_accessibility))
        }
        OutlinedButton(
            onClick = onOpenBatterySettings,
            modifier = Modifier.fillMaxWidth(),
        ) {
          Text(text = stringResource(id = R.string.service_settings_open_battery))
        }
        OutlinedButton(
            onClick = onOpenAppDetails,
            modifier = Modifier.fillMaxWidth(),
        ) {
          Text(text = stringResource(id = R.string.service_settings_open_app_info))
        }
        OutlinedButton(
            onClick = onOpenXiaomiHelp,
            modifier = Modifier.fillMaxWidth(),
        ) {
          Text(text = stringResource(id = R.string.service_settings_open_xiaomi_help))
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

@Composable
private fun StatusValue(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
  Column(
      verticalArrangement = Arrangement.spacedBy(2.dp),
      modifier = modifier.fillMaxWidth(),
  ) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
    )
    Text(
        text = value,
        style = MaterialTheme.typography.bodyLarge,
        maxLines = 3,
        overflow = TextOverflow.Ellipsis,
    )
  }
}

@Composable
private fun formatTimestampOrUnknown(timestampMillis: Long?): String =
    timestampMillis?.let { SETTINGS_TIMESTAMP_FORMATTER.format(Instant.ofEpochMilli(it)) }
        ?: stringResource(id = R.string.service_settings_status_unknown)

private val SETTINGS_TIMESTAMP_FORMATTER: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault())
