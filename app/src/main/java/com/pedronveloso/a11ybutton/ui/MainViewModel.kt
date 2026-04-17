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
import com.pedronveloso.a11ybutton.data.InstalledAppsRepository
import com.pedronveloso.a11ybutton.data.SettingsRepository
import com.pedronveloso.a11ybutton.model.AppSettings
import com.pedronveloso.a11ybutton.model.InstalledApp
import com.pedronveloso.a11ybutton.model.SelectedAppState
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
  private val installedAppsRepository = InstalledAppsRepository(application)
  private val serviceComponent =
      ComponentName(application, ShortcutLaunchAccessibilityService::class.java)
  private val serviceEnabled = MutableStateFlow(false)
  private val selectedAppState = MutableStateFlow<SelectedAppState>(SelectedAppState.None)
  private val availableApps = MutableStateFlow(AppPickerApps())
  private val serviceMessage = MutableStateFlow<String?>(null)
  private val settingsState =
      settingsRepository.settings.stateIn(
          scope = viewModelScope,
          started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
          initialValue = AppSettings(),
      )

  val screenState =
      combine(
              serviceEnabled,
              settingsState,
              selectedAppState,
              serviceMessage,
          ) { isServiceEnabled, settings, currentSelection, currentServiceMessage ->
            deriveMainScreenState(
                serviceEnabled = isServiceEnabled,
                disclosureAccepted = settings.disclosureAccepted,
                selectedAppState = currentSelection,
                serviceMessage = currentServiceMessage,
            )
          }
          .stateIn(
              scope = viewModelScope,
              started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
              initialValue = MainScreenState(),
          )

  val pickerApps =
      availableApps.stateIn(
          scope = viewModelScope,
          started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
          initialValue = AppPickerApps(),
      )

  init {
    refreshServiceStatus()
    refreshAvailableApps()
    viewModelScope.launch {
      settingsState.collect { settings ->
        selectedAppState.value = installedAppsRepository.validateSelection(settings)
      }
    }
  }

  fun refreshServiceStatus() {
    serviceEnabled.value =
        AccessibilityStatusRepository.isServiceEnabled(
            context = getApplication(),
            serviceComponent = serviceComponent,
        )
  }

  fun refreshSelection() {
    selectedAppState.value = installedAppsRepository.validateSelection(settingsState.value)
  }

  fun refreshAvailableApps() {
    availableApps.value =
        AppPickerApps(
            items =
                installedAppsRepository.getLaunchableApps().filterNot {
                  it.packageName == getApplication<Application>().packageName
                },
        )
  }

  fun acceptDisclosure() {
    viewModelScope.launch { settingsRepository.setDisclosureAccepted(accepted = true) }
  }

  fun setServiceMessage(message: String?) {
    serviceMessage.value = message
  }

  fun clearServiceMessage() {
    serviceMessage.value = null
  }

  fun selectApp(app: InstalledApp) {
    viewModelScope.launch {
      settingsRepository.updateSelection(
          packageName = app.packageName,
          componentName = app.componentName,
      )
      refreshSelection()
    }
  }
}
