/*
 * Copyright (C) 2026 Pedro Veloso
 * SPDX-License-Identifier: Apache-2.0
 */
package com.pedronveloso.a11ybutton

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pedronveloso.a11ybutton.data.InstalledAppsRepository
import com.pedronveloso.a11ybutton.logging.InMemoryLogStore
import com.pedronveloso.a11ybutton.logging.LogEntries
import com.pedronveloso.a11ybutton.logging.LogEntry
import com.pedronveloso.a11ybutton.model.InstalledApp
import com.pedronveloso.a11ybutton.model.InvalidSelectionReason
import com.pedronveloso.a11ybutton.model.SelectedAppState
import com.pedronveloso.a11ybutton.notifications.ServiceStatusNotifier
import com.pedronveloso.a11ybutton.service.ServiceDiagnostics
import com.pedronveloso.a11ybutton.service.ServiceDiagnosticsStore
import com.pedronveloso.a11ybutton.ui.AppPickerApps
import com.pedronveloso.a11ybutton.ui.BackgroundProtectionBrand
import com.pedronveloso.a11ybutton.ui.MainScreenState
import com.pedronveloso.a11ybutton.ui.MainViewModel
import com.pedronveloso.a11ybutton.ui.SetupReadiness
import com.pedronveloso.a11ybutton.ui.theme.A11YButtonTheme
import com.pedronveloso.a11ybutton.ui.theme.a11YButtonStatusPalette
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

private enum class MainDestination {
  Home,
  Setup,
  BackgroundProtection,
  Faq,
  Picker,
  DebugMenu,
  DebugLogs,
}

private enum class StatusTone {
  Positive,
  Attention,
}

class MainActivity : ComponentActivity() {
  private val mainViewModel: MainViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    Timber.i("MainActivity created")
    consumeIntent(intent)
    enableEdgeToEdge()
    setContent { A11YButtonTheme { MainRoute(viewModel = mainViewModel) } }
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    Timber.i("MainActivity received a new intent")
    setIntent(intent)
    consumeIntent(intent)
  }

  private fun consumeIntent(intent: Intent?) {
    val serviceMessage = intent?.getStringExtra(EXTRA_STATUS_MESSAGE)
    Timber.d("Consuming activity intent with status message=%s", serviceMessage)
    mainViewModel.setServiceMessage(serviceMessage)
    intent?.removeExtra(EXTRA_STATUS_MESSAGE)
  }

  companion object {
    const val EXTRA_STATUS_MESSAGE = "status_message"
  }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun MainRoute(
    modifier: Modifier = Modifier,
    viewModel: MainViewModel = viewModel(),
) {
  val screenState by viewModel.screenState.collectAsStateWithLifecycle()
  val pickerApps by viewModel.pickerApps.collectAsStateWithLifecycle()
  val logEntries by InMemoryLogStore.entries.collectAsStateWithLifecycle()
  val diagnostics by ServiceDiagnosticsStore.state.collectAsStateWithLifecycle()
  val lifecycleOwner = LocalLifecycleOwner.current
  val context = LocalContext.current
  var destination by rememberSaveable { mutableStateOf(MainDestination.Home) }
  val canOpenDebugTools = BuildConfig.DEBUG

  DisposableEffect(lifecycleOwner, viewModel) {
    val observer = LifecycleEventObserver { _, event ->
      if (event == Lifecycle.Event.ON_RESUME) {
        viewModel.refreshServiceStatus()
        viewModel.refreshSelection()
        viewModel.refreshBackgroundProtectionStatus()
        viewModel.refreshNotificationsEnabled()
        ServiceStatusNotifier.cancelNotification(context)
      }
    }
    lifecycleOwner.lifecycle.addObserver(observer)

    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
  }

  when (destination) {
    MainDestination.Home ->
        Scaffold(
            modifier = modifier.fillMaxSize(),
            topBar = {
              AppTopBar(
                  title = stringResource(id = R.string.app_name),
                  showDebugAction = canOpenDebugTools,
                  onDebugClick = { destination = MainDestination.DebugMenu },
              )
            },
        ) { innerPadding ->
          HomeScreen(
              screenState = screenState,
              onOpenSetup = { destination = MainDestination.Setup },
              onChooseApp = {
                viewModel.refreshAvailableApps()
                destination = MainDestination.Picker
              },
              onOpenFaq = { destination = MainDestination.Faq },
              onDismissServiceMessage = viewModel::clearServiceMessage,
              onEnableNotifications = viewModel::enableNotifications,
              modifier = Modifier.padding(innerPadding),
          )
        }

    MainDestination.Setup -> {
      BackHandler { destination = MainDestination.Home }
      Scaffold(
          modifier = modifier.fillMaxSize(),
          topBar = {
            AppTopBar(
                title = stringResource(id = R.string.setup_title),
                showBack = true,
                onBack = { destination = MainDestination.Home },
                showDebugAction = canOpenDebugTools,
                onDebugClick = { destination = MainDestination.DebugMenu },
            )
          },
      ) { innerPadding ->
        SetupScreen(
            screenState = screenState,
            onAcceptDisclosure = viewModel::acceptDisclosure,
            onChooseApp = {
              viewModel.refreshAvailableApps()
              destination = MainDestination.Picker
            },
            onOpenBackgroundProtection = { destination = MainDestination.BackgroundProtection },
            onOpenAccessibilitySettings = {
              SystemSettingsNavigator.openAccessibilitySettings(context)
            },
            onOpenFaq = { destination = MainDestination.Faq },
            onEnableNotifications = viewModel::enableNotifications,
            modifier = Modifier.padding(innerPadding),
        )
      }
    }

    MainDestination.BackgroundProtection -> {
      BackHandler { destination = MainDestination.Setup }
      Scaffold(
          modifier = modifier.fillMaxSize(),
          topBar = {
            AppTopBar(
                title = stringResource(id = R.string.background_protection_title),
                showBack = true,
                onBack = { destination = MainDestination.Setup },
                showDebugAction = canOpenDebugTools,
                onDebugClick = { destination = MainDestination.DebugMenu },
            )
          },
      ) { innerPadding ->
        BackgroundProtectionScreen(
            screenState = screenState,
            onRequestBatteryOptimizationExemption = {
              SystemSettingsNavigator.requestIgnoreBatteryOptimizations(context)
            },
            onOpenBatterySettings = {
              SystemSettingsNavigator.openBatteryOptimizationSettings(context)
            },
            onConfirmXiaomiLock = viewModel::confirmXiaomiRecentsLock,
            modifier = Modifier.padding(innerPadding),
        )
      }
    }

    MainDestination.Faq -> {
      BackHandler { destination = MainDestination.Home }
      Scaffold(
          modifier = modifier.fillMaxSize(),
          topBar = {
            AppTopBar(
                title = stringResource(id = R.string.faq_title),
                showBack = true,
                onBack = { destination = MainDestination.Home },
                showDebugAction = canOpenDebugTools,
                onDebugClick = { destination = MainDestination.DebugMenu },
            )
          },
      ) { innerPadding ->
        FaqScreen(
            modifier = Modifier.padding(innerPadding),
        )
      }
    }

    MainDestination.Picker -> {
      BackHandler { destination = MainDestination.Home }
      AppPickerScreen(
          apps = pickerApps,
          selectedComponentName =
              (screenState.selectedAppState as? SelectedAppState.Valid)?.app?.componentName,
          onBack = { destination = MainDestination.Home },
          onAppSelected = { app ->
            viewModel.selectApp(app)
            destination = MainDestination.Home
          },
          showDebugAction = canOpenDebugTools,
          onDebugClick = { destination = MainDestination.DebugMenu },
          modifier = modifier,
      )
    }

    MainDestination.DebugMenu -> {
      BackHandler { destination = MainDestination.Home }
      DebugMenuScreen(
          logCount = logEntries.items.size,
          diagnostics = diagnostics,
          onBack = { destination = MainDestination.Home },
          onOpenLogs = { destination = MainDestination.DebugLogs },
          modifier = modifier,
      )
    }

    MainDestination.DebugLogs -> {
      BackHandler { destination = MainDestination.DebugMenu }
      DebugLogsScreen(
          entries = logEntries,
          onBack = { destination = MainDestination.DebugMenu },
          onClearLogs = InMemoryLogStore::clear,
          modifier = modifier,
      )
    }
  }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun AppTopBar(
    title: String,
    showBack: Boolean = false,
    onBack: (() -> Unit)? = null,
    showDebugAction: Boolean = false,
    onDebugClick: (() -> Unit)? = null,
) {
  TopAppBar(
      title = { Text(text = title) },
      navigationIcon = {
        if (showBack && onBack != null) {
          IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(id = R.string.picker_back),
            )
          }
        }
      },
      actions = {
        if (showDebugAction && onDebugClick != null) {
          IconButton(onClick = onDebugClick) {
            Icon(
                imageVector = Icons.Filled.BugReport,
                contentDescription = stringResource(id = R.string.debug_open_menu),
            )
          }
        }
      },
  )
}

@Composable
fun HomeScreen(
    screenState: MainScreenState,
    onOpenSetup: () -> Unit,
    onChooseApp: () -> Unit,
    onOpenFaq: () -> Unit,
    onDismissServiceMessage: () -> Unit,
    onEnableNotifications: () -> Unit,
    modifier: Modifier = Modifier,
) {
  Column(
      verticalArrangement = Arrangement.spacedBy(16.dp),
      modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
  ) {
    screenState.serviceMessage?.let { message ->
      SectionCard(title = stringResource(id = R.string.main_message_title)) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )
        OutlinedButton(
            onClick = onDismissServiceMessage,
            modifier = Modifier.fillMaxWidth(),
        ) {
          Text(text = stringResource(id = R.string.main_message_dismiss))
        }
      }
    }

    StatusSummaryCard(
        screenState = screenState,
        onOpenSetup = onOpenSetup,
    )

    SelectedAppCard(
        selectedAppState = screenState.selectedAppState,
        onChooseApp = onChooseApp,
    )

    if (screenState.isReady) {
      NotificationPermissionCard(
          notificationsEnabled = screenState.notificationsEnabled,
          onEnable = onEnableNotifications,
      )
    }

    SectionCard(title = stringResource(id = R.string.home_support_title)) {
      OutlinedButton(
          onClick = onOpenFaq,
          modifier = Modifier.fillMaxWidth(),
      ) {
        Text(text = stringResource(id = R.string.home_open_faq))
      }
      if (!screenState.isReady) {
        Button(
            onClick = onOpenSetup,
            modifier = Modifier.fillMaxWidth(),
        ) {
          Text(text = stringResource(id = R.string.home_open_setup))
        }
      }
    }
  }
}

@Composable
private fun StatusSummaryCard(
    screenState: MainScreenState,
    onOpenSetup: () -> Unit,
    modifier: Modifier = Modifier,
) {
  val tone = if (screenState.isReady) StatusTone.Positive else StatusTone.Attention
  val colors = statusCardColors(tone)
  Card(
      modifier = modifier.fillMaxWidth(),
      colors =
          CardDefaults.cardColors(
              containerColor = colors.container,
              contentColor = colors.content,
          ),
  ) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.padding(20.dp),
    ) {
      Text(
          text =
              if (screenState.isReady) {
                stringResource(id = R.string.home_ready_title)
              } else {
                stringResource(id = R.string.home_attention_title)
              },
          style = MaterialTheme.typography.headlineSmall,
      )
      Text(
          text =
              if (screenState.isReady) {
                stringResource(id = R.string.home_ready_body)
              } else {
                stringResource(id = R.string.home_attention_body)
              },
          style = MaterialTheme.typography.bodyMedium,
      )
      StatusPillRow(screenState = screenState)
      if (!screenState.isReady) {
        Button(
            onClick = onOpenSetup,
            modifier = Modifier.fillMaxWidth(),
        ) {
          Text(text = stringResource(id = R.string.home_open_setup))
        }
      }
    }
  }
}

@Composable
private fun StatusPillRow(
    screenState: MainScreenState,
    modifier: Modifier = Modifier,
) {
  Column(
      verticalArrangement = Arrangement.spacedBy(12.dp),
      modifier = modifier.fillMaxWidth(),
  ) {
    StatusBadge(
        label = stringResource(id = R.string.main_setup_overall_label),
        value = readinessLabel(screenState.readiness),
        tone = if (screenState.isReady) StatusTone.Positive else StatusTone.Attention,
    )
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
      StatusBadge(
          label = stringResource(id = R.string.main_setup_service_label),
          value =
              if (screenState.serviceEnabled) {
                stringResource(id = R.string.main_status_service_enabled)
              } else {
                stringResource(id = R.string.main_status_service_disabled)
              },
          tone = if (screenState.serviceEnabled) StatusTone.Positive else StatusTone.Attention,
          modifier = Modifier.weight(1f),
      )
      StatusBadge(
          label = stringResource(id = R.string.main_setup_selected_app_label),
          value = selectedAppStatusLabel(screenState.selectedAppState),
          tone =
              if (screenState.selectedAppState is SelectedAppState.Valid) {
                StatusTone.Positive
              } else {
                StatusTone.Attention
              },
          modifier = Modifier.weight(1f),
      )
    }
    if (screenState.backgroundProtection.isRequired) {
      StatusBadge(
          label = stringResource(id = R.string.setup_background_protection_label),
          value = backgroundProtectionStatusLabel(screenState),
          tone =
              if (screenState.backgroundProtection.isComplete) {
                StatusTone.Positive
              } else {
                StatusTone.Attention
              },
      )
    }
  }
}

@Composable
private fun StatusBadge(
    label: String,
    value: String,
    tone: StatusTone,
    modifier: Modifier = Modifier,
) {
  val colors = statusBadgeColors(tone)
  Column(
      verticalArrangement = Arrangement.spacedBy(4.dp),
      modifier =
          modifier
              .background(
                  color = colors.container,
                  shape = RoundedCornerShape(20.dp),
              )
              .padding(horizontal = 14.dp, vertical = 12.dp),
  ) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = colors.content,
    )
    Text(
        text = value,
        style = MaterialTheme.typography.titleSmall,
        color = colors.content,
    )
  }
}

@Composable
private fun SelectedAppCard(
    selectedAppState: SelectedAppState,
    onChooseApp: () -> Unit,
    modifier: Modifier = Modifier,
) {
  SectionCard(
      title = stringResource(id = R.string.main_selected_app_title),
      modifier = modifier,
  ) {
    when (selectedAppState) {
      is SelectedAppState.Valid -> {
        RowWithIcon(
            label = selectedAppState.app.label,
            supportingText = selectedAppState.app.packageName,
            componentName = selectedAppState.app.componentName,
        )
        OutlinedButton(
            onClick = onChooseApp,
            modifier = Modifier.fillMaxWidth(),
        ) {
          Text(text = stringResource(id = R.string.home_selected_app_change))
        }
      }

      is SelectedAppState.Invalid -> {
        Text(
            text = stringResource(id = R.string.main_selected_app_invalid),
            style = MaterialTheme.typography.bodyLarge,
        )
        Text(
            text = invalidSelectionMessage(selectedAppState.reason),
            style = MaterialTheme.typography.bodySmall,
        )
        selectedAppState.packageName?.let { packageName ->
          Text(
              text = stringResource(id = R.string.main_selected_app_package, packageName),
              style = MaterialTheme.typography.bodySmall,
          )
        }
        Button(
            onClick = onChooseApp,
            modifier = Modifier.fillMaxWidth(),
        ) {
          Text(text = stringResource(id = R.string.home_selected_app_change))
        }
      }

      SelectedAppState.None -> {
        Text(
            text = stringResource(id = R.string.main_selected_app_empty),
            style = MaterialTheme.typography.bodyLarge,
        )
        Text(
            text = stringResource(id = R.string.main_selected_app_help),
            style = MaterialTheme.typography.bodySmall,
        )
        Button(
            onClick = onChooseApp,
            modifier = Modifier.fillMaxWidth(),
        ) {
          Text(text = stringResource(id = R.string.home_selected_app_choose))
        }
      }
    }
  }
}

@Composable
private fun SetupScreen(
    screenState: MainScreenState,
    onAcceptDisclosure: () -> Unit,
    onChooseApp: () -> Unit,
    onOpenBackgroundProtection: () -> Unit,
    onOpenAccessibilitySettings: () -> Unit,
    onOpenFaq: () -> Unit,
    onEnableNotifications: () -> Unit,
    modifier: Modifier = Modifier,
) {
  Column(
      verticalArrangement = Arrangement.spacedBy(16.dp),
      modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
  ) {
    Text(
        text = stringResource(id = R.string.setup_intro_title),
        style = MaterialTheme.typography.headlineSmall,
    )
    Text(
        text = stringResource(id = R.string.setup_intro_body),
        style = MaterialTheme.typography.bodyLarge,
    )

    SectionCard(title = stringResource(id = R.string.setup_checklist_title)) {
      StatusRow(
          label = stringResource(id = R.string.main_setup_service_label),
          value =
              if (screenState.serviceEnabled) {
                stringResource(id = R.string.main_status_service_enabled)
              } else {
                stringResource(id = R.string.main_status_service_disabled)
              },
      )
      StatusRow(
          label = stringResource(id = R.string.setup_disclosure_label),
          value =
              if (screenState.disclosureAccepted) {
                stringResource(id = R.string.main_disclosure_accepted)
              } else {
                stringResource(id = R.string.main_status_readiness_not_setup)
              },
      )
      StatusRow(
          label = stringResource(id = R.string.main_setup_selected_app_label),
          value = selectedAppStatusLabel(screenState.selectedAppState),
      )
      if (screenState.backgroundProtection.isRequired) {
        StatusRow(
            label = stringResource(id = R.string.setup_background_protection_label),
            value = backgroundProtectionStatusLabel(screenState),
        )
      }
    }

    SectionCard(title = stringResource(id = R.string.main_disclosure_title)) {
      Text(
          text = stringResource(id = R.string.main_disclosure_body),
          style = MaterialTheme.typography.bodyMedium,
      )
      if (screenState.disclosureAccepted) {
        Text(
            text = stringResource(id = R.string.main_disclosure_accepted),
            style = MaterialTheme.typography.labelLarge,
        )
      } else {
        Button(
            onClick = onAcceptDisclosure,
            modifier = Modifier.fillMaxWidth(),
        ) {
          Text(text = stringResource(id = R.string.setup_action_accept_disclosure))
        }
      }
    }

    SectionCard(title = stringResource(id = R.string.main_actions_title)) {
      Button(
          onClick = onOpenAccessibilitySettings,
          modifier = Modifier.fillMaxWidth(),
      ) {
        Text(text = stringResource(id = R.string.main_action_open_settings))
      }
      OutlinedButton(
          onClick = onChooseApp,
          modifier = Modifier.fillMaxWidth(),
      ) {
        Text(
            text =
                if (screenState.selectedAppState is SelectedAppState.Valid) {
                  stringResource(id = R.string.main_action_change_app)
                } else {
                  stringResource(id = R.string.main_action_choose_app)
                },
        )
      }
      if (screenState.backgroundProtection.isRequired) {
        OutlinedButton(
            onClick = onOpenBackgroundProtection,
            modifier = Modifier.fillMaxWidth(),
        ) {
          Text(text = stringResource(id = R.string.setup_action_open_background_protection))
        }
      }
    }

    NotificationPermissionCard(
        notificationsEnabled = screenState.notificationsEnabled,
        onEnable = onEnableNotifications,
    )

    SectionCard(title = stringResource(id = R.string.setup_primary_help)) {
      OutlinedButton(
          onClick = onOpenFaq,
          modifier = Modifier.fillMaxWidth(),
      ) {
        Text(text = stringResource(id = R.string.home_open_faq))
      }
    }
  }
}

@Composable
private fun NotificationPermissionCard(
    notificationsEnabled: Boolean,
    onEnable: () -> Unit,
    modifier: Modifier = Modifier,
) {
  if (notificationsEnabled) return

  val launcher =
      rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) onEnable()
      }

  SectionCard(
      title = stringResource(id = R.string.setup_notifications_title),
      modifier = modifier,
  ) {
    Text(
        text = stringResource(id = R.string.setup_notifications_body),
        style = MaterialTheme.typography.bodyMedium,
    )
    Button(
        onClick = {
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
          } else {
            onEnable()
          }
        },
        modifier = Modifier.fillMaxWidth(),
    ) {
      Text(text = stringResource(id = R.string.setup_notifications_action))
    }
  }
}

@Composable
private fun BackgroundProtectionScreen(
    screenState: MainScreenState,
    onRequestBatteryOptimizationExemption: () -> Unit,
    onOpenBatterySettings: () -> Unit,
    onConfirmXiaomiLock: () -> Unit,
    modifier: Modifier = Modifier,
) {
  val backgroundProtection = screenState.backgroundProtection
  Column(
      verticalArrangement = Arrangement.spacedBy(16.dp),
      modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
  ) {
    if (!backgroundProtection.isRequired) {
      Text(
          text = stringResource(id = R.string.background_protection_not_required_title),
          style = MaterialTheme.typography.headlineSmall,
      )
      Text(
          text = stringResource(id = R.string.background_protection_not_required_body),
          style = MaterialTheme.typography.bodyLarge,
      )
    } else {
      Text(
          text = stringResource(id = R.string.background_protection_intro_title),
          style = MaterialTheme.typography.headlineSmall,
      )
      Text(
          text = backgroundProtectionIntroBody(backgroundProtection.requiredBrand),
          style = MaterialTheme.typography.bodyLarge,
      )

      SectionCard(title = stringResource(id = R.string.setup_checklist_title)) {
        StatusRow(
            label = stringResource(id = R.string.background_protection_battery_title),
            value =
                if (backgroundProtection.batteryOptimizationIgnored) {
                  stringResource(id = R.string.background_protection_battery_done)
                } else {
                  stringResource(id = R.string.background_protection_battery_pending)
                },
        )
        if (backgroundProtection.requiresRecentsLock) {
          StatusRow(
              label = stringResource(id = R.string.background_protection_xiaomi_lock_title),
              value =
                  if (backgroundProtection.recentsLockConfirmed) {
                    stringResource(id = R.string.background_protection_xiaomi_lock_done)
                  } else {
                    stringResource(id = R.string.background_protection_xiaomi_lock_pending)
                  },
          )
        }
      }

      SectionCard(title = stringResource(id = R.string.background_protection_actions_title)) {
        Button(
            onClick = onRequestBatteryOptimizationExemption,
            modifier = Modifier.fillMaxWidth(),
        ) {
          Text(text = stringResource(id = R.string.background_protection_request_battery_exemption))
        }
        OutlinedButton(
            onClick = onOpenBatterySettings,
            modifier = Modifier.fillMaxWidth(),
        ) {
          Text(text = stringResource(id = R.string.background_protection_open_battery_settings))
        }
      }

      if (backgroundProtection.requiresRecentsLock) {
        SectionCard(title = stringResource(id = R.string.background_protection_xiaomi_title)) {
          Text(
              text = stringResource(id = R.string.background_protection_xiaomi_body),
              style = MaterialTheme.typography.bodyMedium,
          )
          OutlinedButton(
              onClick = onConfirmXiaomiLock,
              modifier = Modifier.fillMaxWidth(),
          ) {
            Text(text = stringResource(id = R.string.background_protection_confirm_xiaomi_lock))
          }
        }
      }
    }
  }
}

@Composable
private fun FaqScreen(
    modifier: Modifier = Modifier,
) {
  Column(
      verticalArrangement = Arrangement.spacedBy(16.dp),
      modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
  ) {
    Text(
        text = stringResource(id = R.string.faq_intro_title),
        style = MaterialTheme.typography.headlineSmall,
    )
    Text(
        text = stringResource(id = R.string.faq_intro_body),
        style = MaterialTheme.typography.bodyLarge,
    )

    FaqEntry(
        question = stringResource(id = R.string.faq_question_button),
        answer = stringResource(id = R.string.main_troubleshooting_button),
    )
    FaqEntry(
        question = stringResource(id = R.string.faq_question_nothing),
        answer = stringResource(id = R.string.main_troubleshooting_nothing),
    )
    FaqEntry(
        question = stringResource(id = R.string.faq_question_missing_app),
        answer = stringResource(id = R.string.main_troubleshooting_missing_app),
    )
    FaqEntry(
        question = stringResource(id = R.string.faq_question_background),
        answer = stringResource(id = R.string.main_troubleshooting_background),
    )
  }
}

@Composable
private fun FaqEntry(
    question: String,
    answer: String,
    modifier: Modifier = Modifier,
) {
  SectionCard(
      title = question,
      modifier = modifier,
  ) {
    Text(
        text = answer,
        style = MaterialTheme.typography.bodyMedium,
    )
  }
}

@Composable
private fun SelectedAppRow(
    app: InstalledApp,
    modifier: Modifier = Modifier,
) {
  RowWithIcon(
      label = app.label,
      supportingText = app.packageName,
      componentName = app.componentName,
      modifier = modifier,
  )
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun AppPickerScreen(
    apps: AppPickerApps,
    selectedComponentName: String?,
    onBack: () -> Unit,
    onAppSelected: (InstalledApp) -> Unit,
    showDebugAction: Boolean,
    onDebugClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
  var query by rememberSaveable { mutableStateOf("") }
  val filteredApps =
      remember(apps, query) {
        apps.items.filter { app ->
          query.isBlank() ||
              app.label.contains(query, ignoreCase = true) ||
              app.packageName.contains(query, ignoreCase = true)
        }
      }

  Scaffold(
      modifier = modifier.fillMaxSize(),
      topBar = {
        AppTopBar(
            title = stringResource(id = R.string.picker_title),
            showBack = true,
            onBack = onBack,
            showDebugAction = showDebugAction,
            onDebugClick = onDebugClick,
        )
      },
  ) { innerPadding ->
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp),
    ) {
      OutlinedTextField(
          value = query,
          onValueChange = { query = it },
          modifier = Modifier.fillMaxWidth(),
          singleLine = true,
          label = { Text(text = stringResource(id = R.string.picker_search_label)) },
          keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
      )
      OutlinedButton(
          onClick = onBack,
          modifier = Modifier.fillMaxWidth(),
      ) {
        Text(text = stringResource(id = R.string.picker_back))
      }

      if (filteredApps.isEmpty()) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize(),
        ) {
          Text(
              text = stringResource(id = R.string.picker_empty),
              style = MaterialTheme.typography.bodyMedium,
          )
        }
      } else {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
          items(
              items = filteredApps,
              key = { it.componentName },
          ) { app ->
            val isSelected = app.componentName == selectedComponentName
            val stateDescription =
                if (isSelected) {
                  contextString(id = R.string.picker_state_selected, argument = app.label)
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
                        .selectable(
                            selected = isSelected,
                            onClick = { onAppSelected(app) },
                            role = androidx.compose.ui.semantics.Role.Button,
                        )
                        .semantics {
                          selected = isSelected
                          this.stateDescription = stateDescription
                        },
            ) {
              Column(modifier = Modifier.padding(16.dp)) {
                RowWithIcon(
                    label = app.label,
                    supportingText = app.packageName,
                    componentName = app.componentName,
                )
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
        }
      }
    }
  }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun DebugMenuScreen(
    logCount: Int,
    diagnostics: ServiceDiagnostics,
    onBack: () -> Unit,
    onOpenLogs: () -> Unit,
    modifier: Modifier = Modifier,
) {
  Scaffold(
      modifier = modifier.fillMaxSize(),
      topBar = {
        AppTopBar(
            title = stringResource(id = R.string.debug_menu_title),
            showBack = true,
            onBack = onBack,
        )
      },
  ) { innerPadding ->
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize().padding(innerPadding).padding(24.dp),
    ) {
      SectionCard(title = stringResource(id = R.string.debug_menu_title)) {
        Text(
            text = stringResource(id = R.string.debug_menu_logs_count, logCount),
            style = MaterialTheme.typography.bodyMedium,
        )
        Button(
            onClick = onOpenLogs,
            modifier = Modifier.fillMaxWidth(),
        ) {
          Text(text = stringResource(id = R.string.debug_menu_open_logs))
        }
      }
      SectionCard(title = stringResource(id = R.string.debug_diagnostics_title)) {
        StatusRow(
            label = stringResource(id = R.string.debug_service_connection_label),
            value =
                if (diagnostics.serviceConnected) {
                  stringResource(id = R.string.debug_service_connection_connected)
                } else {
                  stringResource(id = R.string.debug_service_connection_disconnected)
                },
        )
        StatusRow(
            label = stringResource(id = R.string.debug_accessibility_button_label),
            value = accessibilityButtonAvailabilityLabel(diagnostics.accessibilityButtonAvailable),
        )
        StatusRow(
            label = stringResource(id = R.string.debug_last_event_label),
            value = diagnostics.lastLifecycleEvent ?: stringResource(id = R.string.debug_unknown),
        )
        StatusRow(
            label = stringResource(id = R.string.debug_last_event_at_label),
            value = formatDebugTimestampOrUnknown(diagnostics.lastLifecycleEventAtMillis),
        )
        StatusRow(
            label = stringResource(id = R.string.debug_last_button_press_label),
            value = formatDebugTimestampOrUnknown(diagnostics.lastButtonPressAtMillis),
        )
        StatusRow(
            label = stringResource(id = R.string.debug_last_trigger_label),
            value = formatDebugTimestampOrUnknown(diagnostics.lastTriggerAtMillis),
        )
      }
    }
  }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun DebugLogsScreen(
    entries: LogEntries,
    onBack: () -> Unit,
    onClearLogs: () -> Unit,
    modifier: Modifier = Modifier,
) {
  Scaffold(
      modifier = modifier.fillMaxSize(),
      topBar = {
        AppTopBar(
            title = stringResource(id = R.string.debug_logs_title),
            showBack = true,
            onBack = onBack,
        )
      },
  ) { innerPadding ->
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp),
    ) {
      OutlinedButton(
          onClick = onClearLogs,
          modifier = Modifier.fillMaxWidth(),
      ) {
        Text(text = stringResource(id = R.string.debug_logs_clear))
      }

      if (entries.items.isEmpty()) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize(),
        ) {
          Text(
              text = stringResource(id = R.string.debug_logs_empty),
              style = MaterialTheme.typography.bodyMedium,
          )
        }
      } else {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
          items(
              items = entries.items.asReversed(),
              key = { entry ->
                "${entry.timestampMillis}:${entry.priority}:${entry.tag}:${entry.message}"
              },
          ) { entry ->
            LogEntryCard(entry = entry)
          }
        }
      }
    }
  }
}

@Composable
private fun LogEntryCard(
    entry: LogEntry,
    modifier: Modifier = Modifier,
) {
  Card(modifier = modifier.fillMaxWidth()) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(16.dp),
    ) {
      Row(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier.fillMaxWidth(),
      ) {
        Text(
            text = formatLogPriority(entry.priority),
            style = MaterialTheme.typography.labelLarge,
        )
        Text(
            text = formatLogTimestamp(entry.timestampMillis),
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
      }
      Text(
          text = entry.tag ?: stringResource(id = R.string.debug_log_entry_fallback_tag),
          style = MaterialTheme.typography.labelMedium,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
      )
      Text(
          text = entry.message,
          style = MaterialTheme.typography.bodyMedium,
          maxLines = 6,
          overflow = TextOverflow.Ellipsis,
      )
      entry.throwable?.let { throwable ->
        Text(
            text = "${throwable::class.java.simpleName}: ${throwable.message.orEmpty()}",
            style = MaterialTheme.typography.bodySmall,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
      }
    }
  }
}

@Composable
private fun RowWithIcon(
    label: String,
    supportingText: String,
    componentName: String,
    modifier: Modifier = Modifier,
) {
  Column(
      verticalArrangement = Arrangement.spacedBy(12.dp),
      modifier = modifier.fillMaxWidth(),
  ) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
      AppIcon(
          componentName = componentName,
          contentDescription = null,
      )
      Column(
          verticalArrangement = Arrangement.spacedBy(2.dp),
          modifier = Modifier.weight(1f),
      ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = supportingText,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
      }
    }
    HorizontalDivider()
  }
}

@Composable
private fun AppIcon(
    componentName: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val repository = remember(context) { InstalledAppsRepository(context) }
  val iconBitmap by
      produceState(initialValue = null as android.graphics.Bitmap?, componentName) {
        value =
            withContext(Dispatchers.IO) { repository.loadIconBitmap(componentName, sizePx = 96) }
      }

  if (iconBitmap != null) {
    Image(
        bitmap = iconBitmap!!.asImageBitmap(),
        contentDescription = contentDescription,
        modifier = modifier.size(48.dp),
    )
  } else {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(48.dp).padding(4.dp),
    ) {
      Text(text = stringResource(id = R.string.picker_icon_fallback))
    }
  }
}

@Composable
private fun SectionCard(
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
private fun StatusRow(
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
    )
  }
}

@Composable
private fun statusCardColors(tone: StatusTone): StatusColors =
    when (tone) {
      StatusTone.Positive ->
          StatusColors(
              container = a11YButtonStatusPalette().positiveContainer,
              content = a11YButtonStatusPalette().positiveContent,
          )

      StatusTone.Attention ->
          StatusColors(
              container = MaterialTheme.colorScheme.errorContainer,
              content = MaterialTheme.colorScheme.onErrorContainer,
          )
    }

@Composable
private fun statusBadgeColors(tone: StatusTone): StatusColors {
  val base = statusCardColors(tone)
  return StatusColors(
      container = base.content.copy(alpha = 0.10f),
      content = base.content,
  )
}

private data class StatusColors(
    val container: Color,
    val content: Color,
)

@Composable
private fun readinessLabel(readiness: SetupReadiness): String =
    when (readiness) {
      SetupReadiness.NotSetUp -> stringResource(id = R.string.main_status_readiness_not_setup)
      SetupReadiness.PartiallySetUp -> stringResource(id = R.string.main_status_readiness_partial)
      SetupReadiness.Ready -> stringResource(id = R.string.main_status_readiness_ready)
    }

@Composable
private fun backgroundProtectionStatusLabel(screenState: MainScreenState): String {
  val backgroundProtection = screenState.backgroundProtection
  return when {
    !backgroundProtection.isRequired ->
        stringResource(id = R.string.background_protection_not_required_status)
    backgroundProtection.isComplete -> stringResource(id = R.string.main_status_readiness_ready)
    else -> stringResource(id = R.string.background_protection_pending_status)
  }
}

@Composable
private fun backgroundProtectionIntroBody(requiredBrand: BackgroundProtectionBrand?): String =
    when (requiredBrand) {
      BackgroundProtectionBrand.Xiaomi ->
          stringResource(id = R.string.background_protection_intro_body_xiaomi)
      BackgroundProtectionBrand.Huawei ->
          stringResource(id = R.string.background_protection_intro_body_huawei)
      null -> stringResource(id = R.string.background_protection_not_required_body)
    }

@Composable
private fun selectedAppStatusLabel(selectedAppState: SelectedAppState): String =
    when (selectedAppState) {
      is SelectedAppState.Valid -> stringResource(id = R.string.main_status_app_selected)
      is SelectedAppState.Invalid -> stringResource(id = R.string.main_status_app_invalid)
      SelectedAppState.None -> stringResource(id = R.string.main_status_app_not_selected)
    }

@Composable
private fun invalidSelectionMessage(reason: InvalidSelectionReason): String =
    when (reason) {
      InvalidSelectionReason.MissingApp -> stringResource(id = R.string.main_selected_app_missing)
      InvalidSelectionReason.DisabledApp -> stringResource(id = R.string.main_selected_app_disabled)
      InvalidSelectionReason.MissingComponent ->
          stringResource(id = R.string.main_selected_app_changed)
      InvalidSelectionReason.NotLaunchable ->
          stringResource(id = R.string.main_selected_app_not_launchable)
    }

@Composable
private fun contextString(
    id: Int,
    argument: String,
): String = stringResource(id = id, argument)

private fun formatLogPriority(priority: Int): String =
    when (priority) {
      android.util.Log.VERBOSE -> "V"
      android.util.Log.DEBUG -> "D"
      android.util.Log.INFO -> "I"
      android.util.Log.WARN -> "W"
      android.util.Log.ERROR -> "E"
      android.util.Log.ASSERT -> "A"
      else -> priority.toString()
    }

private fun formatLogTimestamp(timestampMillis: Long): String =
    LOG_TIMESTAMP_FORMATTER.format(Instant.ofEpochMilli(timestampMillis))

@Composable
private fun accessibilityButtonAvailabilityLabel(isAvailable: Boolean?): String =
    when (isAvailable) {
      true -> stringResource(id = R.string.debug_accessibility_button_available)
      false -> stringResource(id = R.string.debug_accessibility_button_unavailable)
      null -> stringResource(id = R.string.debug_unknown)
    }

@Composable
private fun formatDebugTimestampOrUnknown(timestampMillis: Long?): String =
    timestampMillis?.let(::formatLogTimestamp) ?: stringResource(id = R.string.debug_unknown)

private val LOG_TIMESTAMP_FORMATTER: DateTimeFormatter =
    DateTimeFormatter.ofPattern("HH:mm:ss.SSS").withZone(ZoneId.systemDefault())

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
  A11YButtonTheme {
    HomeScreen(
        screenState =
            MainScreenState(
                serviceEnabled = true,
                disclosureAccepted = true,
                selectedAppState =
                    SelectedAppState.Valid(
                        app =
                            InstalledApp(
                                packageName = "com.example.reader",
                                componentName = "com.example.reader/.HomeActivity",
                                label = "Reader",
                            ),
                    ),
                readiness = SetupReadiness.Ready,
            ),
        onOpenSetup = {},
        onChooseApp = {},
        onOpenFaq = {},
        onDismissServiceMessage = {},
        onEnableNotifications = {},
    )
  }
}

@Preview(showBackground = true)
@Composable
private fun SetupScreenPreview() {
  A11YButtonTheme {
    SetupScreen(
        screenState = MainScreenState(),
        onAcceptDisclosure = {},
        onChooseApp = {},
        onOpenBackgroundProtection = {},
        onOpenAccessibilitySettings = {},
        onOpenFaq = {},
        onEnableNotifications = {},
    )
  }
}

@Preview(showBackground = true)
@Composable
private fun AppPickerPreview() {
  A11YButtonTheme {
    AppPickerScreen(
        apps =
            AppPickerApps(
                items =
                    listOf(
                        InstalledApp(
                            packageName = "com.example.reader",
                            componentName = "com.example.reader/.HomeActivity",
                            label = "Reader",
                        ),
                        InstalledApp(
                            packageName = "com.example.mail",
                            componentName = "com.example.mail/.InboxActivity",
                            label = "Mail",
                        ),
                    ),
            ),
        selectedComponentName = "com.example.reader/.HomeActivity",
        onBack = {},
        onAppSelected = {},
        showDebugAction = true,
        onDebugClick = {},
    )
  }
}
