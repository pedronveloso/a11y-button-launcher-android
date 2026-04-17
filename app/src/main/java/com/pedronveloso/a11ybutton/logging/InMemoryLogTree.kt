/*
 * Copyright (C) 2026 Pedro Veloso
 * All rights reserved.
 */
package com.pedronveloso.a11ybutton.logging

import timber.log.Timber

class InMemoryLogTree : Timber.Tree() {
  override fun log(
      priority: Int,
      tag: String?,
      message: String,
      t: Throwable?,
  ) {
    InMemoryLogStore.append(
        LogEntry(
            timestampMillis = System.currentTimeMillis(),
            priority = priority,
            tag = tag,
            message = message,
            throwable = t,
        ),
    )
  }
}
