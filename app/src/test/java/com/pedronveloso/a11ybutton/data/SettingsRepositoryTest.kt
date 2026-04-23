/*
 * Copyright (C) 2026 Pedro Veloso
 * SPDX-License-Identifier: Apache-2.0
 */
package com.pedronveloso.a11ybutton.data

import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.mutablePreferencesOf
import com.pedronveloso.a11ybutton.model.AppSettings
import com.pedronveloso.a11ybutton.model.NotificationPreference
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
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
            SettingsRepository.NOTIFICATION_PREFERENCE_KEY to NotificationPreference.Enabled.name,
        )

    val settings = SettingsRepository.preferencesToAppSettings(preferences)

    assertEquals(
        AppSettings(
            selectedPackageName = "com.example.reader",
            selectedComponentName = "com.example.reader/.HomeActivity",
            disclosureAccepted = true,
            notificationPreference = NotificationPreference.Enabled,
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

  @Test
  fun enableNotifications_setsEnabledAndClearsOptOut() = runTest {
    val repository = createRepository()

    repository.optOutNotifications()

    repository.enableNotifications()

    val settings = repository.settings.first()
    assertEquals(NotificationPreference.Enabled, settings.notificationPreference)
  }

  @Test
  fun preferencesToAppSettings_readsLegacyNotificationFlags() {
    val preferences: MutablePreferences =
        mutablePreferencesOf(SettingsRepository.NOTIFICATIONS_OPTED_OUT_KEY to true)

    val settings = SettingsRepository.preferencesToAppSettings(preferences)

    assertEquals(NotificationPreference.OptedOut, settings.notificationPreference)
  }

  private fun createRepository(): SettingsRepository {
    val file = File.createTempFile("settings-repository-test", ".preferences_pb")
    file.deleteOnExit()
    return SettingsRepository(
        PreferenceDataStoreFactory.create(produceFile = { file }),
    )
  }
}
