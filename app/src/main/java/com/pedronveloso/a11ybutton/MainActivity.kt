/*
 * Copyright (C) 2026 Pedro Veloso
 * All rights reserved.
 */
package com.pedronveloso.a11ybutton

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.viewModels
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pedronveloso.a11ybutton.data.InstalledAppsRepository
import com.pedronveloso.a11ybutton.model.InstalledApp
import com.pedronveloso.a11ybutton.model.InvalidSelectionReason
import com.pedronveloso.a11ybutton.model.SelectedAppState
import com.pedronveloso.a11ybutton.ui.AppPickerApps
import com.pedronveloso.a11ybutton.ui.MainScreenState
import com.pedronveloso.a11ybutton.ui.MainViewModel
import com.pedronveloso.a11ybutton.ui.SetupReadiness
import com.pedronveloso.a11ybutton.ui.theme.A11YButtonTheme

private enum class MainDestination {
    Home,
    Picker,
}

class MainActivity : ComponentActivity() {
    private val mainViewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        consumeIntent(intent)
        enableEdgeToEdge()
        setContent {
            A11YButtonTheme {
                MainRoute(viewModel = mainViewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        consumeIntent(intent)
    }

    private fun consumeIntent(intent: Intent?) {
        mainViewModel.setServiceMessage(intent?.getStringExtra(EXTRA_STATUS_MESSAGE))
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
    val lifecycleOwner = LocalLifecycleOwner.current
    var destination by rememberSaveable { mutableStateOf(MainDestination.Home) }

    DisposableEffect(lifecycleOwner, viewModel) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    viewModel.refreshServiceStatus()
                    viewModel.refreshSelection()
                    viewModel.refreshAvailableApps()
                }
            }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    when (destination) {
        MainDestination.Home ->
            Scaffold(
                modifier = modifier.fillMaxSize(),
                topBar = {
                    TopAppBar(title = { Text(text = stringResource(id = R.string.app_name)) })
                },
            ) { innerPadding ->
                HomeScreen(
                    screenState = screenState,
                    onAcceptDisclosure = viewModel::acceptDisclosure,
                    onChooseApp = {
                        viewModel.refreshAvailableApps()
                        destination = MainDestination.Picker
                    },
                    onDismissServiceMessage = viewModel::clearServiceMessage,
                    modifier = Modifier.padding(innerPadding),
                )
            }

        MainDestination.Picker -> {
            BackHandler { destination = MainDestination.Home }
            AppPickerScreen(
                apps = pickerApps,
                onBack = { destination = MainDestination.Home },
                onAppSelected = { app ->
                    viewModel.selectApp(app)
                    destination = MainDestination.Home
                },
                modifier = modifier,
            )
        }
    }
}

@Composable
fun HomeScreen(
    screenState: MainScreenState,
    onAcceptDisclosure: () -> Unit,
    onChooseApp: () -> Unit,
    onDismissServiceMessage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
    ) {
        Text(
            text = stringResource(id = R.string.main_header_title),
            style = MaterialTheme.typography.headlineSmall,
        )
        Text(
            text = stringResource(id = R.string.main_header_body),
            style = MaterialTheme.typography.bodyLarge,
        )

        screenState.serviceMessage?.let { message ->
            SectionCard(title = stringResource(id = R.string.main_message_title)) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                )
                OutlinedButton(
                    onClick = onDismissServiceMessage,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(text = stringResource(id = R.string.main_message_dismiss))
                }
            }
        }

        SectionCard(title = stringResource(id = R.string.main_setup_title)) {
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
                label = stringResource(id = R.string.main_setup_selected_app_label),
                value =
                    when (screenState.selectedAppState) {
                        is SelectedAppState.Valid ->
                            stringResource(id = R.string.main_status_app_selected)
                        is SelectedAppState.Invalid ->
                            stringResource(id = R.string.main_status_app_invalid)
                        SelectedAppState.None ->
                            stringResource(id = R.string.main_status_app_not_selected)
                    },
            )
            StatusRow(
                label = stringResource(id = R.string.main_setup_overall_label),
                value =
                    when (screenState.readiness) {
                        SetupReadiness.NotSetUp ->
                            stringResource(id = R.string.main_status_readiness_not_setup)
                        SetupReadiness.PartiallySetUp ->
                            stringResource(id = R.string.main_status_readiness_partial)
                        SetupReadiness.Ready ->
                            stringResource(id = R.string.main_status_readiness_ready)
                    },
            )
        }

        SectionCard(title = stringResource(id = R.string.main_actions_title)) {
            Button(
                onClick = {
                    context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                },
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
                    Text(text = stringResource(id = R.string.main_disclosure_accept))
                }
            }
        }

        SectionCard(title = stringResource(id = R.string.main_selected_app_title)) {
            when (val selectedAppState = screenState.selectedAppState) {
                is SelectedAppState.Valid -> {
                    SelectedAppRow(app = selectedAppState.app)
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
                }
            }
        }

        SectionCard(title = stringResource(id = R.string.main_help_title)) {
            Text(
                text = stringResource(id = R.string.main_help_body),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
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
    onBack: () -> Unit,
    onAppSelected: (InstalledApp) -> Unit,
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
            TopAppBar(
                title = { Text(text = stringResource(id = R.string.picker_title)) },
            )
        },
    ) { innerPadding ->
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
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
                        Card(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clickable { onAppSelected(app) },
                        ) {
                            RowWithIcon(
                                label = app.label,
                                supportingText = app.packageName,
                                componentName = app.componentName,
                                modifier = Modifier.padding(16.dp),
                            )
                        }
                    }
                }
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
        androidx.compose.foundation.layout.Row(
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
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = supportingText,
                    style = MaterialTheme.typography.bodySmall,
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
    val iconBitmap by produceState(initialValue = null as android.graphics.Bitmap?, componentName) {
        value = repository.loadIcon(componentName)?.toBitmap(width = 96, height = 96)
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
            modifier =
                modifier
                    .size(48.dp)
                    .padding(4.dp),
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
private fun invalidSelectionMessage(reason: InvalidSelectionReason): String =
    when (reason) {
        InvalidSelectionReason.MissingApp ->
            stringResource(id = R.string.main_selected_app_missing)
        InvalidSelectionReason.DisabledApp ->
            stringResource(id = R.string.main_selected_app_disabled)
        InvalidSelectionReason.MissingComponent ->
            stringResource(id = R.string.main_selected_app_changed)
        InvalidSelectionReason.NotLaunchable ->
            stringResource(id = R.string.main_selected_app_not_launchable)
    }

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
            onAcceptDisclosure = {},
            onChooseApp = {},
            onDismissServiceMessage = {},
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
            onBack = {},
            onAppSelected = {},
        )
    }
}
