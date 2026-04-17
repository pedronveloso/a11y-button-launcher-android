/*
 * Copyright (C) 2026 Pedro Veloso
 * All rights reserved.
 */
package com.pedronveloso.a11ybutton.service

import android.accessibilityservice.AccessibilityButtonController
import android.accessibilityservice.AccessibilityService
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent

class ShortcutLaunchAccessibilityService : AccessibilityService() {
    private val callbackHandler = Handler(Looper.getMainLooper())
    private val accessibilityButtonCallback =
        object : AccessibilityButtonController.AccessibilityButtonCallback() {
            override fun onClicked(controller: AccessibilityButtonController) {
                // Phase 1 only wires the callback. Launch handling is added later.
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
        super.onDestroy()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // No event processing is needed for this app.
    }

    override fun onInterrupt() {
        // No long-running feedback is active.
    }
}
