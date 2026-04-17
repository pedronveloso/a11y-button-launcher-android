/*
 * Copyright (C) 2026 Pedro Veloso
 * All rights reserved.
 */
package com.pedronveloso.a11ybutton.ui

import com.pedronveloso.a11ybutton.model.InstalledApp
import com.pedronveloso.a11ybutton.model.InvalidSelectionReason
import com.pedronveloso.a11ybutton.model.SelectedAppState
import org.junit.Assert.assertEquals
import org.junit.Test

class MainScreenStateTest {
    @Test
    fun deriveMainScreenState_isNotSetUp_whenNothingIsConfigured() {
        val state =
            deriveMainScreenState(
                serviceEnabled = false,
                disclosureAccepted = false,
                selectedAppState = SelectedAppState.None,
                serviceMessage = null,
            )

        assertEquals(SetupReadiness.NotSetUp, state.readiness)
    }

    @Test
    fun deriveMainScreenState_isPartiallySetUp_whenOnlyServiceIsEnabled() {
        val state =
            deriveMainScreenState(
                serviceEnabled = true,
                disclosureAccepted = true,
                selectedAppState = SelectedAppState.None,
                serviceMessage = null,
            )

        assertEquals(SetupReadiness.PartiallySetUp, state.readiness)
    }

    @Test
    fun deriveMainScreenState_isReady_whenServiceAndSelectionExist() {
        val state =
            deriveMainScreenState(
                serviceEnabled = true,
                disclosureAccepted = true,
                selectedAppState =
                    SelectedAppState.Valid(
                        app =
                            InstalledApp(
                                packageName = "com.example.reader",
                                componentName = "com.example.reader/.HomeActivity",
                                label = "Reader",
                            ),
                    ),
                serviceMessage = null,
            )

        assertEquals(SetupReadiness.Ready, state.readiness)
    }

    @Test
    fun deriveMainScreenState_isNotReady_whenSelectionIsInvalid() {
        val state =
            deriveMainScreenState(
                serviceEnabled = true,
                disclosureAccepted = true,
                selectedAppState =
                    SelectedAppState.Invalid(
                        packageName = "com.example.reader",
                        componentName = "com.example.reader/.HomeActivity",
                        reason = InvalidSelectionReason.MissingApp,
                    ),
                serviceMessage = null,
            )

        assertEquals(SetupReadiness.PartiallySetUp, state.readiness)
    }
}
