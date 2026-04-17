/*
 * Copyright (C) 2026 Pedro Veloso
 * All rights reserved.
 */
package com.pedronveloso.a11ybutton

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pedronveloso.a11ybutton.model.FoundationStatus
import com.pedronveloso.a11ybutton.ui.MainViewModel
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
    val foundationStatus by viewModel.foundationStatus.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(title = { Text(text = stringResource(id = R.string.app_name)) })
        },
    ) { innerPadding ->
        HomeScreen(
            foundationStatus = foundationStatus,
            modifier = Modifier.padding(innerPadding),
        )
    }
}

@Composable
fun HomeScreen(
    foundationStatus: FoundationStatus,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier =
            modifier
                .fillMaxSize()
                .padding(24.dp),
    ) {
        Text(text = stringResource(id = R.string.main_status_title))
        Card {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(16.dp),
            ) {
                Text(
                    text =
                        if (foundationStatus.serviceEnabled) {
                            stringResource(id = R.string.main_status_service_enabled)
                        } else {
                            stringResource(id = R.string.main_status_service_disabled)
                        },
                )
                Text(
                    text =
                        if (foundationStatus.disclosureAccepted) {
                            stringResource(id = R.string.main_status_disclosure_accepted)
                        } else {
                            stringResource(id = R.string.main_status_disclosure_pending)
                        },
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    A11YButtonTheme {
        HomeScreen(
            foundationStatus =
                FoundationStatus(
                    serviceEnabled = true,
                    disclosureAccepted = true,
                ),
        )
    }
}
