/*
 * Copyright (C) 2026 Pedro Veloso
 * All rights reserved.
 */
package com.pedronveloso.a11ybutton.logging

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

object InMemoryLogStore {
  private const val MAX_ENTRIES = 300

  private val _entries = MutableStateFlow<List<LogEntry>>(emptyList())
  val entries: StateFlow<List<LogEntry>> = _entries.asStateFlow()

  fun append(entry: LogEntry) {
    _entries.update { existing -> (existing + entry).takeLast(MAX_ENTRIES) }
  }

  fun clear() {
    _entries.value = emptyList()
  }
}
