/*
 * Copyright (C) 2026 Pedro Veloso
 * SPDX-License-Identifier: Apache-2.0
 */
package com.pedronveloso.a11ybutton.ui

import com.pedronveloso.a11ybutton.model.InstalledApp
import com.pedronveloso.a11ybutton.model.SelectedAppState
import org.junit.Assert.assertEquals
import org.junit.Test

class SetupStatusItemsTest {
  @Test
  fun setupStatusItems_listsEveryRequirement_onAnyDevice() {
    val items = MainScreenState().setupStatusItems()

    assertEquals(
        listOf(
            SetupItem.Disclosure,
            SetupItem.Service,
            SetupItem.ButtonAction,
            SetupItem.Battery,
        ),
        items.map { it.item },
    )
    assertEquals(listOf(false, false, false, false), items.map { it.isDone })
  }

  @Test
  fun setupStatusItems_addsRecentsLock_onXiaomiOnly() {
    val xiaomi =
        MainScreenState(
            backgroundProtection =
                BackgroundProtectionState(requiredBrand = BackgroundProtectionBrand.Xiaomi),
        )
    val huawei =
        MainScreenState(
            backgroundProtection =
                BackgroundProtectionState(requiredBrand = BackgroundProtectionBrand.Huawei),
        )

    assertEquals(SetupItem.RecentsLock, xiaomi.setupStatusItems().last().item)
    assertEquals(SetupItem.Battery, huawei.setupStatusItems().last().item)
  }

  @Test
  fun setupStatusItems_marksCompletedRequirements() {
    val state =
        MainScreenState(
            disclosureAccepted = true,
            serviceEnabled = true,
            selectedAppState =
                SelectedAppState.Valid(InstalledApp("com.example.a", "com.example.a/.Main", "A")),
            backgroundProtection = BackgroundProtectionState(batteryOptimizationIgnored = false),
        )

    assertEquals(listOf(true, true, true, false), state.setupStatusItems().map { it.isDone })
  }

  @Test
  fun setupStatusItems_allDone_matchesDerivedReadiness() {
    val protection = BackgroundProtectionState(batteryOptimizationIgnored = true)
    val state =
        deriveMainScreenState(
            serviceEnabled = true,
            disclosureAccepted = true,
            selectedAppState =
                SelectedAppState.Valid(InstalledApp("com.example.a", "com.example.a/.Main", "A")),
            backgroundProtection = protection,
        )

    assertEquals(true, state.setupStatusItems().all { it.isDone })
    assertEquals(SetupReadiness.Ready, state.readiness)
  }
}
