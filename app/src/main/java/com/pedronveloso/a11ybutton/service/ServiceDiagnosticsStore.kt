/*
 * Copyright (C) 2026 Pedro Veloso
 * SPDX-License-Identifier: Apache-2.0
 */
package com.pedronveloso.a11ybutton.service

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class ServiceDiagnostics(
    val serviceConnected: Boolean = false,
    val accessibilityButtonAvailable: Boolean? = null,
    val lastLifecycleEvent: String? = null,
    val lastLifecycleEventAtMillis: Long? = null,
    val lastButtonPressAtMillis: Long? = null,
    val lastTriggerAtMillis: Long? = null,
)

object ServiceDiagnosticsStore {
  private val _state = MutableStateFlow(ServiceDiagnostics())
  val state: StateFlow<ServiceDiagnostics> = _state.asStateFlow()

  fun recordServiceConnected() {
    mutate { copy(serviceConnected = true, lastLifecycleEvent = "connected") }
  }

  fun recordServiceDestroyed() {
    mutate {
      copy(
          serviceConnected = false,
          accessibilityButtonAvailable = false,
          lastLifecycleEvent = "destroyed",
      )
    }
  }

  fun recordServiceUnbound() {
    mutate {
      copy(
          serviceConnected = false,
          accessibilityButtonAvailable = false,
          lastLifecycleEvent = "unbound",
      )
    }
  }

  fun recordServiceInterrupted() {
    mutate { copy(lastLifecycleEvent = "interrupted") }
  }

  fun recordButtonAvailability(isAvailable: Boolean) {
    mutate {
      copy(
          accessibilityButtonAvailable = isAvailable,
          lastLifecycleEvent = "button availability changed",
      )
    }
  }

  fun recordButtonPressed() {
    val now = System.currentTimeMillis()
    _state.update { current ->
      current.copy(
          accessibilityButtonAvailable = true,
          lastButtonPressAtMillis = now,
      )
    }
  }

  fun recordTriggerHandled() {
    val now = System.currentTimeMillis()
    _state.update { current -> current.copy(lastTriggerAtMillis = now) }
  }

  private fun mutate(update: ServiceDiagnostics.() -> ServiceDiagnostics) {
    val now = System.currentTimeMillis()
    _state.update { current -> current.update().copy(lastLifecycleEventAtMillis = now) }
  }
}
