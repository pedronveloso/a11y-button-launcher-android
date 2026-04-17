/*
 * Copyright (C) 2026 Pedro Veloso
 * All rights reserved.
 */
package com.pedronveloso.a11ybutton.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class MainScreenStateTest {
    @Test
    fun deriveMainScreenState_isNotSetUp_whenNothingIsConfigured() {
        val state =
            deriveMainScreenState(
                serviceEnabled = false,
                disclosureAccepted = false,
                selectedAppConfigured = false,
            )

        assertEquals(SetupReadiness.NotSetUp, state.readiness)
    }

    @Test
    fun deriveMainScreenState_isPartiallySetUp_whenOnlyServiceIsEnabled() {
        val state =
            deriveMainScreenState(
                serviceEnabled = true,
                disclosureAccepted = true,
                selectedAppConfigured = false,
            )

        assertEquals(SetupReadiness.PartiallySetUp, state.readiness)
    }

    @Test
    fun deriveMainScreenState_isReady_whenServiceAndSelectionExist() {
        val state =
            deriveMainScreenState(
                serviceEnabled = true,
                disclosureAccepted = true,
                selectedAppConfigured = true,
            )

        assertEquals(SetupReadiness.Ready, state.readiness)
    }
}
