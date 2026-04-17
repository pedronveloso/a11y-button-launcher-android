/*
 * Copyright (C) 2026 Pedro Veloso
 * All rights reserved.
 */
package com.pedronveloso.a11ybutton.service

import android.accessibilityservice.AccessibilityButtonController
import android.accessibilityservice.AccessibilityService
import android.content.ActivityNotFoundException
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

class ShortcutLaunchAccessibilityService : AccessibilityService() {
    private val callbackHandler = Handler(Looper.getMainLooper())
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val accessibilityButtonCallback =
        object : AccessibilityButtonController.AccessibilityButtonCallback() {
            override fun onClicked(controller: AccessibilityButtonController) {
                handleTrigger()
            }
        }

    override fun onServiceConnected() {
        super.onServiceConnected()

        accessibilityButtonController.registerAccessibilityButtonCallback(
            accessibilityButtonCallback,
            callbackHandler,
        )
    }

    override fun onDestroy() {
        accessibilityButtonController.unregisterAccessibilityButtonCallback(accessibilityButtonCallback)
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // No event processing is needed for this app.
    }

    override fun onInterrupt() {
        // No long-running feedback is active.
    }

    private fun handleTrigger() {
        serviceScope.launch {
            val settingsRepository = SettingsRepository.fromContext(this@ShortcutLaunchAccessibilityService)
            val installedAppsRepository = InstalledAppsRepository(this@ShortcutLaunchAccessibilityService)
            val settings = settingsRepository.settings.first()
            val selectionState = installedAppsRepository.validateSelection(settings)

            when (selectionState) {
                is SelectedAppState.Valid -> {
                    launchTargetApp(selectionState.app.componentName)
                }

                is SelectedAppState.Invalid -> {
                    settingsRepository.updateSelection(
                        packageName = null,
                        componentName = null,
                    )
                    openHostApp(
                        message = getString(R.string.service_message_invalid_selection),
                    )
                }

                SelectedAppState.None -> {
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
            openHostApp(message = getString(R.string.service_message_launch_failed))
            return
        }

        try {
            startActivity(launchIntent)
        } catch (_: ActivityNotFoundException) {
            openHostApp(message = getString(R.string.service_message_launch_failed))
        } catch (_: SecurityException) {
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
