/*
 * Copyright (C) 2026 Pedro Veloso
 * All rights reserved.
 */
package com.pedronveloso.a11ybutton.ui

enum class SetupReadiness {
    NotSetUp,
    PartiallySetUp,
    Ready,
}

data class MainScreenState(
    val serviceEnabled: Boolean = false,
    val disclosureAccepted: Boolean = false,
    val selectedAppConfigured: Boolean = false,
    val readiness: SetupReadiness = SetupReadiness.NotSetUp,
)

fun deriveMainScreenState(
    serviceEnabled: Boolean,
    disclosureAccepted: Boolean,
    selectedAppConfigured: Boolean,
): MainScreenState {
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
        selectedAppConfigured = selectedAppConfigured,
        readiness = readiness,
    )
}
