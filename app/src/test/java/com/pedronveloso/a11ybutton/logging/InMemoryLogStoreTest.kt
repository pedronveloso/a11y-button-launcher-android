/*
 * Copyright (C) 2026 Pedro Veloso
 * SPDX-License-Identifier: Apache-2.0
 */
package com.pedronveloso.a11ybutton.logging

import org.junit.Assert.assertEquals
import org.junit.Test

class InMemoryLogStoreTest {
  @Test
  fun append_addsEntriesInOrder() {
    InMemoryLogStore.clear()

    val firstEntry =
        LogEntry(
            timestampMillis = 1L,
            priority = android.util.Log.INFO,
            tag = "First",
            message = "one",
            throwable = null,
        )
    val secondEntry =
        LogEntry(
            timestampMillis = 2L,
            priority = android.util.Log.WARN,
            tag = "Second",
            message = "two",
            throwable = null,
        )

    InMemoryLogStore.append(firstEntry)
    InMemoryLogStore.append(secondEntry)

    assertEquals(listOf(firstEntry, secondEntry), InMemoryLogStore.entries.value.items)
  }

  @Test
  fun append_keepsOnlyMostRecentEntries() {
    InMemoryLogStore.clear()

    repeat(305) { index ->
      InMemoryLogStore.append(
          LogEntry(
              timestampMillis = index.toLong(),
              priority = android.util.Log.DEBUG,
              tag = "Tag$index",
              message = "message-$index",
              throwable = null,
          ),
      )
    }

    val entries = InMemoryLogStore.entries.value.items

    assertEquals(300, entries.size)
    assertEquals("message-5", entries.first().message)
    assertEquals("message-304", entries.last().message)
  }
}
