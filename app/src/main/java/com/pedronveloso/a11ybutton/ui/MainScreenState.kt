/*
 * Copyright (C) 2026 Pedro Veloso
 * All rights reserved.
 */
package com.pedronveloso.a11ybutton.ui

import com.pedronveloso.a11ybutton.model.SelectedAppState

enum class SetupReadiness {
    NotSetUp,
    PartiallySetUp,
    Ready,
}

data class MainScreenState(
    val serviceEnabled: Boolean = false,
    val disclosureAccepted: Boolean = false,
    val selectedAppState: SelectedAppState = SelectedAppState.None,
    val serviceMessage: String? = null,
    val readiness: SetupReadiness = SetupReadiness.NotSetUp,
)

fun deriveMainScreenState(
    serviceEnabled: Boolean,
    disclosureAccepted: Boolean,
    selectedAppState: SelectedAppState,
    serviceMessage: String? = null,
): MainScreenState {
    val selectedAppConfigured = selectedAppState is SelectedAppState.Valid
    val completedRequirements = listOf(serviceEnabled, selectedAppConfigured).count { it }
    val readiness =
        when (completedRequirements) {
            2 -> SetupReadiness.Ready
            1 -> SetupReadiness.PartiallySetUp
            else -> SetupReadiness.NotSetUp
        }

    return MainScreenState(
        serviceEnabled = serviceEnabled,
        disclosureAccepted = disclosureAccepted,
        selectedAppState = selectedAppState,
        serviceMessage = serviceMessage,
        readiness = readiness,
    )
}
