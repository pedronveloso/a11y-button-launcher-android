/*
 * Copyright (C) 2026 Pedro Veloso
 * All rights reserved.
 */
package com.pedronveloso.a11ybutton

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pedronveloso.a11ybutton.ui.MainScreenState
import com.pedronveloso.a11ybutton.ui.MainViewModel
import com.pedronveloso.a11ybutton.ui.SetupReadiness
import com.pedronveloso.a11ybutton.ui.theme.A11YButtonTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            A11YButtonTheme {
                MainRoute()
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun MainRoute(
    modifier: Modifier = Modifier,
    viewModel: MainViewModel = viewModel(),
) {
    val screenState by viewModel.screenState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner, viewModel) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    viewModel.refreshServiceStatus()
                }
            }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(title = { Text(text = stringResource(id = R.string.app_name)) })
        },
    ) { innerPadding ->
        HomeScreen(
            screenState = screenState,
            onAcceptDisclosure = viewModel::acceptDisclosure,
            modifier = Modifier.padding(innerPadding),
        )
    }
}

@Composable
fun HomeScreen(
    screenState: MainScreenState,
    onAcceptDisclosure: () -> Unit,
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
                    if (screenState.selectedAppConfigured) {
                        stringResource(id = R.string.main_status_app_selected)
                    } else {
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
                onClick = {},
                enabled = false,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = stringResource(id = R.string.main_action_choose_app))
            }
            Text(
                text = stringResource(id = R.string.main_action_choose_app_pending),
                style = MaterialTheme.typography.bodySmall,
            )
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
            Text(
                text = stringResource(id = R.string.main_selected_app_empty),
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = stringResource(id = R.string.main_selected_app_help),
                style = MaterialTheme.typography.bodySmall,
            )
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

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    A11YButtonTheme {
        HomeScreen(
            screenState =
                MainScreenState(
                    serviceEnabled = true,
                    disclosureAccepted = true,
                    selectedAppConfigured = false,
                    readiness = SetupReadiness.PartiallySetUp,
                ),
            onAcceptDisclosure = {},
        )
    }
}
