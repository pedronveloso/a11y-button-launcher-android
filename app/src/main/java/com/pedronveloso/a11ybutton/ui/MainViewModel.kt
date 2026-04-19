/*
 * Copyright (C) 2026 Pedro Veloso
 * SPDX-License-Identifier: Apache-2.0
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
import timber.log.Timber

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
    Timber.i("MainViewModel initialized")
    refreshServiceStatus()
    refreshAvailableApps()
    viewModelScope.launch {
      settingsState.collect { settings ->
        Timber.d(
            "Settings updated with package=%s component=%s disclosureAccepted=%s",
            settings.selectedPackageName,
            settings.selectedComponentName,
            settings.disclosureAccepted,
        )
        selectedAppState.value = installedAppsRepository.validateSelection(settings)
      }
    }
  }

  fun refreshServiceStatus() {
    Timber.d("Refreshing accessibility service status")
    serviceEnabled.value =
        AccessibilityStatusRepository.isServiceEnabled(
            context = getApplication(),
            serviceComponent = serviceComponent,
        )
  }

  fun refreshSelection() {
    Timber.d("Refreshing selected app state")
    selectedAppState.value = installedAppsRepository.validateSelection(settingsState.value)
  }

  fun refreshAvailableApps() {
    Timber.d("Refreshing available launchable apps")
    availableApps.value =
        AppPickerApps(
            items =
                installedAppsRepository.getLaunchableApps().filterNot {
                  it.packageName == getApplication<Application>().packageName
                },
        )
  }

  fun acceptDisclosure() {
    Timber.i("Disclosure accepted from main screen")
    viewModelScope.launch { settingsRepository.setDisclosureAccepted(accepted = true) }
  }

  fun setServiceMessage(message: String?) {
    Timber.i("Updating service message to %s", message)
    serviceMessage.value = message
  }

  fun clearServiceMessage() {
    Timber.d("Clearing service message")
    serviceMessage.value = null
  }

  fun selectApp(app: InstalledApp) {
    Timber.i("Selected app changed to package=%s component=%s", app.packageName, app.componentName)
    viewModelScope.launch {
      settingsRepository.updateSelection(
          packageName = app.packageName,
          componentName = app.componentName,
      )
      refreshSelection()
    }
  }
}
