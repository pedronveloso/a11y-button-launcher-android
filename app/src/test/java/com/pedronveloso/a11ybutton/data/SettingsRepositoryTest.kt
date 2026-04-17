/*
 * Copyright (C) 2026 Pedro Veloso
 * All rights reserved.
 */
package com.pedronveloso.a11ybutton.data

import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.mutablePreferencesOf
import com.pedronveloso.a11ybutton.model.AppSettings
import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsRepositoryTest {
  @Test
  fun preferencesToAppSettings_returnsDefaults_whenPreferencesAreEmpty() {
    val settings = SettingsRepository.preferencesToAppSettings(emptyPreferences())

    assertEquals(AppSettings(), settings)
  }

  @Test
  fun preferencesToAppSettings_mapsStoredValues() {
    val preferences: MutablePreferences =
        mutablePreferencesOf(
            SettingsRepository.SELECTED_PACKAGE_NAME_KEY to "com.example.reader",
            SettingsRepository.SELECTED_COMPONENT_NAME_KEY to "com.example.reader/.HomeActivity",
            SettingsRepository.DISCLOSURE_ACCEPTED_KEY to true,
        )

    val settings = SettingsRepository.preferencesToAppSettings(preferences)

    assertEquals(
        AppSettings(
            selectedPackageName = "com.example.reader",
            selectedComponentName = "com.example.reader/.HomeActivity",
            disclosureAccepted = true,
        ),
        settings,
    )
  }

  @Test
  fun preferencesToAppSettings_treatsBlankSelectionAsNull() {
    val preferences: MutablePreferences =
        mutablePreferencesOf(
            SettingsRepository.SELECTED_PACKAGE_NAME_KEY to "",
            SettingsRepository.SELECTED_COMPONENT_NAME_KEY to " ",
        )

    val settings = SettingsRepository.preferencesToAppSettings(preferences)

    assertEquals(
        AppSettings(
            selectedPackageName = null,
            selectedComponentName = null,
            disclosureAccepted = false,
        ),
        settings,
    )
  }
}
