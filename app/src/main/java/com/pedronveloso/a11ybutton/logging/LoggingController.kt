/*
 * Copyright (C) 2026 Pedro Veloso
 * SPDX-License-Identifier: Apache-2.0
 */
package com.pedronveloso.a11ybutton.logging

import android.content.Context
import com.pedronveloso.a11ybutton.data.SettingsRepository
import com.pedronveloso.logviewer.StandardLogRedactor
import com.pedronveloso.logviewer.TimberLogCapture
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class LoggingState(
    val enabled: Boolean = false,
    val transitioning: Boolean = true,
    val error: Boolean = false,
)

/** Owns a single capture for the process; transitions outlive the activity. */
class LoggingController(
    context: Context,
    private val repository: SettingsRepository,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
) {
  private val appContext = context.applicationContext
  private val mutex = Mutex()
  private val mutableState = MutableStateFlow(LoggingState())
  val state = mutableState.asStateFlow()
  var capture: TimberLogCapture? = null
    private set

  init {
    // Only this controller writes the logging settings, so the persisted state is applied once at
    // startup. State starts as transitioning, which keeps setEnabled out until this finishes.
    scope.launch {
      mutex.withLock {
        try {
          val settings = repository.settings.first()
          if (settings.loggingCleanupPending) clearRetainedSessions()
          if (settings.inAppLoggingEnabled) ensureCapture().install() else capture?.uninstall()
          mutableState.value =
              LoggingState(enabled = settings.inAppLoggingEnabled, transitioning = false)
        } catch (failure: Exception) {
          if (failure is CancellationException) throw failure
          capture?.uninstall()
          mutableState.value = LoggingState(transitioning = false, error = true)
        }
      }
    }
  }

  fun setEnabled(enabled: Boolean) {
    val before = mutableState.value
    if (
        before.transitioning ||
            !mutableState.compareAndSet(before, before.copy(transitioning = true))
    ) {
      return
    }
    scope.launch {
      mutex.withLock {
        try {
          if (!enabled) {
            capture?.uninstall()
            mutableState.value = mutableState.value.copy(enabled = false)
            repository.setInAppLoggingEnabled(false)
            clearRetainedSessions()
          } else {
            if (repository.settings.first().loggingCleanupPending) clearRetainedSessions()
            repository.setInAppLoggingEnabled(true)
            ensureCapture().install()
          }
          mutableState.value = LoggingState(enabled = enabled, transitioning = false)
        } catch (failure: Exception) {
          if (failure is CancellationException) throw failure
          capture?.uninstall()
          mutableState.value = LoggingState(transitioning = false, error = true)
        }
      }
    }
  }

  private fun ensureCapture(): TimberLogCapture =
      capture
          ?: TimberLogCapture(
                  appContext,
                  persistAcrossCrashes = true,
                  redact = StandardLogRedactor::redact,
              )
              .also { capture = it }

  private suspend fun clearRetainedSessions() {
    val source = ensureCapture()
    source.uninstall()
    // sessions() awaits journal initialization, including buffered startup writes.
    val sessions = source.sessions()
    sessions.forEach { source.clear(it.id) }
    check(source.health.value.readError == null && source.health.value.writeError == null) {
      "Could not clear retained logs"
    }
    check(source.sessions().all { it.isCurrent && source.entries(it.id).isEmpty() }) {
      "Retained logs remain"
    }
    repository.finishLoggingCleanup()
  }
}
