/*
 * Copyright (C) 2026 Pedro Veloso
 * SPDX-License-Identifier: Apache-2.0
 */
package com.pedronveloso.a11ybutton.ui

import com.pedronveloso.a11ybutton.model.isConfigured

enum class SetupItem {
  Disclosure,
  Service,
  ButtonAction,
  Battery,
  RecentsLock,
}

data class SetupStatusItem(
    val item: SetupItem,
    val isDone: Boolean,
)

/** One entry per requirement that feeds [MainScreenState.readiness], in the order to fix them. */
fun MainScreenState.setupStatusItems(): List<SetupStatusItem> = buildList {
  add(SetupStatusItem(SetupItem.Disclosure, disclosureAccepted))
  add(SetupStatusItem(SetupItem.Service, serviceEnabled))
  add(SetupStatusItem(SetupItem.ButtonAction, selectedAppState.isConfigured))
  add(SetupStatusItem(SetupItem.Battery, backgroundProtection.batteryOptimizationIgnored))
  if (backgroundProtection.requiresRecentsLock) {
    add(SetupStatusItem(SetupItem.RecentsLock, backgroundProtection.recentsLockConfirmed))
  }
}
