/*
 * Copyright (C) 2026 Pedro Veloso
 * SPDX-License-Identifier: Apache-2.0
 */
package com.pedronveloso.a11ybutton.logging

import androidx.compose.runtime.Immutable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class LogEntry(
    val timestampMillis: Long,
    val priority: Int,
    val tag: String?,
    val message: String,
    val throwable: Throwable?,
)

@Immutable
data class LogEntries(
    val items: List<LogEntry> = emptyList(),
)

object InMemoryLogStore {
  private const val MAX_ENTRIES = 300

  private val _entries = MutableStateFlow(LogEntries())
  val entries: StateFlow<LogEntries> = _entries.asStateFlow()

  fun append(entry: LogEntry) {
    _entries.update { existing ->
      LogEntries(items = (existing.items + entry).takeLast(MAX_ENTRIES))
    }
  }

  fun clear() {
    _entries.value = LogEntries()
  }
}
