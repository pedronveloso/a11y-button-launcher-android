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
import com.pedronveloso.a11ybutton.model.FoundationStatus
import com.pedronveloso.a11ybutton.service.ShortcutLaunchAccessibilityService
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class MainViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val settingsRepository = SettingsRepository.fromContext(application)
    private val accessibilityStatusRepository = AccessibilityStatusRepository(application)
    private val serviceComponent =
        ComponentName(application, ShortcutLaunchAccessibilityService::class.java)

    val foundationStatus =
        combine(
            accessibilityStatusRepository.observeServiceEnabled(serviceComponent),
            settingsRepository.settings,
        ) { serviceEnabled, settings ->
            FoundationStatus(
                serviceEnabled = serviceEnabled,
                disclosureAccepted = settings.disclosureAccepted,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = FoundationStatus(),
        )
}
