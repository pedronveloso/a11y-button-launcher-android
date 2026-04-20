/*
 * Copyright (C) 2026 Pedro Veloso
 * SPDX-License-Identifier: Apache-2.0
 */
package com.pedronveloso.a11ybutton.service

import android.accessibilityservice.AccessibilityButtonController
import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import com.pedronveloso.a11ybutton.R
import com.pedronveloso.a11ybutton.data.InstalledAppsRepository
import com.pedronveloso.a11ybutton.data.SettingsRepository
import com.pedronveloso.a11ybutton.model.SelectedAppState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber

class ShortcutLaunchAccessibilityService : AccessibilityService() {
  private val callbackHandler = Handler(Looper.getMainLooper())
  private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
  private val accessibilityButtonCallback =
      object : AccessibilityButtonController.AccessibilityButtonCallback() {
        override fun onClicked(controller: AccessibilityButtonController) {
          ServiceDiagnosticsStore.recordButtonPressed()
          Timber.i("Accessibility button pressed")
          handleTrigger()
        }

        override fun onAvailabilityChanged(
            controller: AccessibilityButtonController,
            available: Boolean,
        ) {
          ServiceDiagnosticsStore.recordButtonAvailability(available)
          Timber.i("Accessibility button availability changed to %s", available)
        }
      }

  override fun onServiceConnected() {
    super.onServiceConnected()
    serviceInfo =
        serviceInfo.apply {
          eventTypes = 0
          notificationTimeout = 0
          feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
        }
    ServiceDiagnosticsStore.recordServiceConnected()
    Timber.i("Accessibility service connected")
    val isButtonAvailable = accessibilityButtonController.isAccessibilityButtonAvailable
    Timber.i("Accessibility button availability sampled on connect=%s", isButtonAvailable)

    accessibilityButtonController.registerAccessibilityButtonCallback(
        accessibilityButtonCallback,
        callbackHandler,
    )
  }

  override fun onDestroy() {
    ServiceDiagnosticsStore.recordServiceDestroyed()
    Timber.i("Accessibility service destroyed")
    accessibilityButtonController.unregisterAccessibilityButtonCallback(accessibilityButtonCallback)
    serviceScope.cancel()
    super.onDestroy()
  }

  override fun onAccessibilityEvent(event: AccessibilityEvent?) {
    // No event processing is needed for this app.
  }

  override fun onInterrupt() {
    ServiceDiagnosticsStore.recordServiceInterrupted()
    Timber.w("Accessibility service interrupted")
    // No long-running feedback is active.
  }

  override fun onUnbind(intent: Intent): Boolean {
    ServiceDiagnosticsStore.recordServiceUnbound()
    Timber.w("Accessibility service unbound")
    return super.onUnbind(intent)
  }

  private fun handleTrigger() {
    ServiceDiagnosticsStore.recordTriggerHandled()
    Timber.i("Accessibility shortcut triggered")
    serviceScope.launch {
      val settingsRepository =
          SettingsRepository.fromContext(this@ShortcutLaunchAccessibilityService)
      val installedAppsRepository = InstalledAppsRepository(this@ShortcutLaunchAccessibilityService)
      val settings = settingsRepository.settings.first()
      Timber.d(
          "Loaded settings for trigger with package=%s component=%s disclosureAccepted=%s",
          settings.selectedPackageName,
          settings.selectedComponentName,
          settings.disclosureAccepted,
      )
      val selectionState = installedAppsRepository.validateSelection(settings)

      when (selectionState) {
        is SelectedAppState.Valid -> {
          Timber.i(
              "Attempting to launch selected app label=%s package=%s component=%s",
              selectionState.app.label,
              selectionState.app.packageName,
              selectionState.app.componentName,
          )
          launchTargetApp(selectionState.app.componentName)
        }

        is SelectedAppState.Invalid -> {
          Timber.w(
              "Saved selection invalid; clearing package=%s component=%s",
              selectionState.packageName,
              selectionState.componentName,
          )
          settingsRepository.updateSelection(
              packageName = null,
              componentName = null,
          )
          openHostApp(
              message = getString(R.string.service_message_invalid_selection),
          )
        }

        SelectedAppState.None -> {
          Timber.i("No app has been selected yet; opening host app")
          openHostApp(
              message = getString(R.string.service_message_choose_app_first),
          )
        }
      }
    }
  }

  private fun launchTargetApp(componentName: String) {
    val launchIntent = LaunchIntentFactory.createTargetAppIntent(componentName)
    if (launchIntent == null) {
      Timber.e("Failed to build launch intent for component=%s", componentName)
      openHostApp(message = getString(R.string.service_message_launch_failed))
      return
    }

    try {
      startActivity(launchIntent)
      Timber.i("Target app launch started for component=%s", componentName)
    } catch (exception: ActivityNotFoundException) {
      Timber.e(exception, "Target activity was not found for component=%s", componentName)
      openHostApp(message = getString(R.string.service_message_launch_failed))
    } catch (exception: SecurityException) {
      Timber.e(exception, "Security exception when launching component=%s", componentName)
      openHostApp(message = getString(R.string.service_message_launch_failed))
    }
  }

  private fun openHostApp(message: String) {
    startActivity(
        LaunchIntentFactory.createHostAppIntent(
            context = this,
            message = message,
        ),
    )
  }
}
