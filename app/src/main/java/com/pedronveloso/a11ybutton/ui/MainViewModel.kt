/*
 * Copyright (C) 2026 Pedro Veloso
 * All rights reserved.
 */
package com.pedronveloso.a11ybutton.ui

import android.app.Application
import android.content.ComponentName
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pedronveloso.a11ybutton.data.AccessibilityStatusRepository
import com.pedronveloso.a11ybutton.data.SettingsRepository
import com.pedronveloso.a11ybutton.service.ShortcutLaunchAccessibilityService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val settingsRepository = SettingsRepository.fromContext(application)
    private val accessibilityStatusRepository = AccessibilityStatusRepository(application)
    private val serviceComponent =
        ComponentName(application, ShortcutLaunchAccessibilityService::class.java)
    private val serviceEnabled = MutableStateFlow(false)

    val screenState =
        combine(
            serviceEnabled,
            settingsRepository.settings,
        ) { isServiceEnabled, settings ->
            deriveMainScreenState(
                serviceEnabled = isServiceEnabled,
                disclosureAccepted = settings.disclosureAccepted,
                selectedAppConfigured =
                    settings.selectedPackageName != null && settings.selectedComponentName != null,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = MainScreenState(),
        )

    init {
        refreshServiceStatus()
    }

    fun refreshServiceStatus() {
        serviceEnabled.value =
            AccessibilityStatusRepository.isServiceEnabled(
                context = getApplication(),
                serviceComponent = serviceComponent,
            )
    }

    fun acceptDisclosure() {
        viewModelScope.launch {
            settingsRepository.setDisclosureAccepted(accepted = true)
        }
    }
}
