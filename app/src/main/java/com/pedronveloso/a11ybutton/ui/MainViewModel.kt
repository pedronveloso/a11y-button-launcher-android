/*
 * Copyright (C) 2026 Pedro Veloso
 * SPDX-License-Identifier: Apache-2.0
 */
package com.pedronveloso.a11ybutton.ui

import android.Manifest
import android.app.Application
import android.content.ComponentName
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.pedronveloso.a11ybutton.SystemSettingsNavigator
import com.pedronveloso.a11ybutton.data.AccessibilityStatusRepository
import com.pedronveloso.a11ybutton.data.InstalledAppsRepository
import com.pedronveloso.a11ybutton.data.SettingsRepository
import com.pedronveloso.a11ybutton.model.AppSettings
import com.pedronveloso.a11ybutton.model.InstalledApp
import com.pedronveloso.a11ybutton.model.NotificationPreference
import com.pedronveloso.a11ybutton.model.SelectedAppState
import com.pedronveloso.a11ybutton.model.ThemeMode
import com.pedronveloso.a11ybutton.service.ShortcutLaunchAccessibilityService
import com.pedronveloso.a11ybutton.work.ServiceCheckWorker
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

class MainViewModel(
    application: Application,
) : AndroidViewModel(application) {
  private data class SelectionSettings(
      val packageName: String?,
      val componentName: String?,
  )

  private val settingsRepository = SettingsRepository.fromContext(application)
  private val installedAppsRepository = InstalledAppsRepository(application)
  private val serviceComponent =
      ComponentName(application, ShortcutLaunchAccessibilityService::class.java)
  private val serviceEnabled = MutableStateFlow(false)
  private val selectedAppState = MutableStateFlow<SelectedAppState>(SelectedAppState.None)
  private val availableApps = MutableStateFlow(AppPickerApps())
  private val serviceMessage = MutableStateFlow<String?>(null)
  private val batteryOptimizationIgnored = MutableStateFlow(false)
  private val backgroundProtectionBrand =
      BackgroundProtectionBrand.fromDevice(
          brand = Build.BRAND,
          manufacturer = Build.MANUFACTURER,
      )
  private val settingsState =
      settingsRepository.settings.stateIn(
          scope = viewModelScope,
          started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
          initialValue = AppSettings(),
      )

  val themeMode: StateFlow<ThemeMode> =
      settingsState
          .map { it.themeMode }
          .stateIn(
              scope = viewModelScope,
              started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
              initialValue = ThemeMode.SYSTEM,
          )

  val screenState =
      combine(
              serviceEnabled,
              settingsState,
              selectedAppState,
              serviceMessage,
              batteryOptimizationIgnored,
          ) { isServiceEnabled, settings, currentSelection, currentServiceMessage, isBatteryIgnored
            ->
            deriveMainScreenState(
                serviceEnabled = isServiceEnabled,
                disclosureAccepted = settings.disclosureAccepted,
                selectedAppState = currentSelection,
                backgroundProtection =
                    BackgroundProtectionState(
                        requiredBrand = backgroundProtectionBrand,
                        batteryOptimizationIgnored = isBatteryIgnored,
                        recentsLockConfirmed = settings.xiaomiRecentsLockConfirmed,
                    ),
                serviceMessage = currentServiceMessage,
                notificationPreference = settings.notificationPreference,
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
    refreshBackgroundProtectionStatus()
    viewModelScope.launch {
      settingsState
          .map { settings ->
            SelectionSettings(
                packageName = settings.selectedPackageName,
                componentName = settings.selectedComponentName,
            )
          }
          .distinctUntilChanged()
          .collectLatest { settings ->
            Timber.d(
                "Selection updated with package=%s component=%s",
                settings.packageName,
                settings.componentName,
            )
            selectedAppState.value =
                withContext(Dispatchers.IO) {
                  installedAppsRepository.validateSelection(
                      AppSettings(
                          selectedPackageName = settings.packageName,
                          selectedComponentName = settings.componentName,
                      ),
                  )
                }
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
    viewModelScope.launch {
      selectedAppState.value =
          withContext(Dispatchers.IO) {
            installedAppsRepository.validateSelection(settingsState.value)
          }
    }
  }

  fun refreshBackgroundProtectionStatus() {
    Timber.d("Refreshing battery optimization state")
    batteryOptimizationIgnored.value =
        SystemSettingsNavigator.isIgnoringBatteryOptimizations(getApplication())
  }

  fun refreshAvailableApps() {
    Timber.d("Refreshing available launchable apps")
    viewModelScope.launch {
      availableApps.value =
          withContext(Dispatchers.IO) {
            AppPickerApps(
                items =
                    installedAppsRepository.getLaunchableApps().filterNot {
                      it.packageName == getApplication<Application>().packageName
                    },
            )
          }
    }
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

  fun refreshNotificationsEnabled() {
    val app = getApplication<Application>()
    val osGranted =
        NotificationManagerCompat.from(app).areNotificationsEnabled() &&
            (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(app, Manifest.permission.POST_NOTIFICATIONS) ==
                    PackageManager.PERMISSION_GRANTED)
    if (
        !osGranted && settingsState.value.notificationPreference == NotificationPreference.Enabled
    ) {
      Timber.i("OS notification permission revoked; disabling notifications preference")
      viewModelScope.launch { settingsRepository.disableNotifications() }
    }
  }

  fun enableNotifications() {
    Timber.i("User opted in to background service monitoring")
    viewModelScope.launch {
      settingsRepository.enableNotifications()
      WorkManager.getInstance(getApplication())
          .enqueueUniquePeriodicWork(
              ServiceCheckWorker.UNIQUE_WORK_NAME,
              ExistingPeriodicWorkPolicy.KEEP,
              PeriodicWorkRequestBuilder<ServiceCheckWorker>(6, TimeUnit.HOURS).build(),
          )
    }
  }

  fun setThemeMode(mode: ThemeMode) {
    viewModelScope.launch { settingsRepository.setThemeMode(mode) }
  }

  fun confirmXiaomiRecentsLock() {
    if (backgroundProtectionBrand != BackgroundProtectionBrand.Xiaomi) {
      return
    }
    Timber.i("Marking Xiaomi recents lock step as confirmed")
    viewModelScope.launch { settingsRepository.setXiaomiRecentsLockConfirmed(confirmed = true) }
  }
}
