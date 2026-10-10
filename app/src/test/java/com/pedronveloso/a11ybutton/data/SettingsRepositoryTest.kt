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
import com.pedronveloso.a11ybutton.model.ShortcutTarget
import com.pedronveloso.a11ybutton.model.ThemeMode
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
            onboardingCompleted = true,
        ),
        settings,
    )
  }

  @Test
  fun preferencesToAppSettings_skipsOnboarding_forPeopleWhoAlreadyAcceptedTheDisclosure() {
    val preferences = mutablePreferencesOf(SettingsRepository.DISCLOSURE_ACCEPTED_KEY to true)

    assertEquals(true, SettingsRepository.preferencesToAppSettings(preferences).onboardingCompleted)
  }

  @Test
  fun preferencesToAppSettings_showsOnboarding_onAFreshInstall() {
    val settings = SettingsRepository.preferencesToAppSettings(emptyPreferences())

    assertEquals(false, settings.onboardingCompleted)
  }

  @Test
  fun preferencesToAppSettings_prefersStoredOnboardingFlagOverDisclosure() {
    val preferences =
        mutablePreferencesOf(
            SettingsRepository.DISCLOSURE_ACCEPTED_KEY to true,
            SettingsRepository.ONBOARDING_COMPLETED_KEY to false,
        )

    assertEquals(
        false,
        SettingsRepository.preferencesToAppSettings(preferences).onboardingCompleted,
    )
  }

  @Test
  fun skipOnboarding_beforeAcceptingDisclosure_survivesDataStoreRecreation() = runTest {
    val file = File.createTempFile("onboarding-settings", ".preferences_pb")
    file.deleteOnExit()
    val storeJob = Job()
    val store =
        PreferenceDataStoreFactory.create(
            scope = CoroutineScope(Dispatchers.IO + storeJob),
            produceFile = { file },
        )
    try {
      val repository = SettingsRepository(store)
      repository.updateShortcutSelection(SHORTCUT)
      repository.setThemeMode(ThemeMode.DARK)
      val before = repository.settings.first()

      repository.setOnboardingCompleted(true)
      storeJob.cancelAndJoin()

      val reopened =
          SettingsRepository(
              PreferenceDataStoreFactory.create(
                  scope = CoroutineScope(backgroundScope.coroutineContext + Dispatchers.IO),
                  produceFile = { file },
              )
          )
      assertEquals(before.copy(onboardingCompleted = true), reopened.settings.first())
    } finally {
      storeJob.cancelAndJoin()
    }
  }

  @Test
  fun setDisclosureAccepted_duringOnboarding_doesNotCompleteOnboarding() = runTest {
    val repository = createRepository()

    repository.setDisclosureAccepted(true)

    val settings = repository.settings.first()
    assertEquals(true, settings.disclosureAccepted)
    assertEquals(false, settings.onboardingCompleted)
  }

  @Test
  fun setDisclosureAccepted_keepsCompletedOnboarding() = runTest {
    val repository = createRepository()
    repository.setOnboardingCompleted(true)

    repository.setDisclosureAccepted(true)

    assertEquals(true, repository.settings.first().onboardingCompleted)
  }

  @Test
  fun setOnboardingCompleted_false_preservesOtherSettings() = runTest {
    val repository = createRepository()
    repository.setDisclosureAccepted(true)
    repository.updateShortcutSelection(SHORTCUT)
    repository.setThemeMode(ThemeMode.DARK)
    repository.enableNotifications()
    repository.setOnboardingCompleted(true)
    val before = repository.settings.first()

    repository.setOnboardingCompleted(false)

    assertEquals(before.copy(onboardingCompleted = false), repository.settings.first())
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
  fun preferencesToAppSettings_dropsShortcutWithBlankLabel() {
    val preferences: MutablePreferences =
        mutablePreferencesOf(
            SettingsRepository.SELECTED_SHORTCUT_PACKAGE_KEY to "com.example.mail",
            SettingsRepository.SELECTED_SHORTCUT_LABEL_KEY to " ",
            SettingsRepository.SELECTED_SHORTCUT_INTENT_URI_KEY to
                "intent:#Intent;package=com.example.mail;end",
        )

    assertNull(SettingsRepository.preferencesToAppSettings(preferences).selectedShortcut)
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

  @Test
  fun preferencesToAppSettings_prefersOptOutWhenLegacyFlagsAreBothTrue() {
    val preferences: MutablePreferences =
        mutablePreferencesOf(
            SettingsRepository.NOTIFICATIONS_ENABLED_KEY to true,
            SettingsRepository.NOTIFICATIONS_OPTED_OUT_KEY to true,
        )

    val settings = SettingsRepository.preferencesToAppSettings(preferences)

    assertEquals(NotificationPreference.OptedOut, settings.notificationPreference)
  }

  @Test
  fun updateShortcutSelection_replacesSelectedApp() = runTest {
    val repository = createRepository()
    repository.updateSelection("com.example.reader", "com.example.reader/.HomeActivity")

    repository.updateShortcutSelection(SHORTCUT)

    val settings = repository.settings.first()
    assertEquals(SHORTCUT, settings.selectedShortcut)
    assertNull(settings.selectedPackageName)
    assertNull(settings.selectedComponentName)
  }

  @Test
  fun updateSelection_replacesSelectedShortcut() = runTest {
    val repository = createRepository()
    repository.updateShortcutSelection(SHORTCUT)

    repository.updateSelection("com.example.reader", "com.example.reader/.HomeActivity")

    val settings = repository.settings.first()
    assertNull(settings.selectedShortcut)
    assertEquals("com.example.reader", settings.selectedPackageName)
  }

  @Test
  fun clearSelection_clearsShortcut() = runTest {
    val repository = createRepository()
    repository.updateShortcutSelection(SHORTCUT)

    repository.clearSelection()

    assertEquals(AppSettings(), repository.settings.first())
  }

  @Test
  fun preferencesToAppSettings_ignoresShortcutMissingItsIntent() {
    val preferences: MutablePreferences =
        mutablePreferencesOf(SettingsRepository.SELECTED_SHORTCUT_PACKAGE_KEY to "com.example.mail")

    val settings = SettingsRepository.preferencesToAppSettings(preferences)

    assertNull(settings.selectedShortcut)
  }

  @Test
  fun logging_defaultsMatchBuildUnlessSaved() {
    for (default in listOf(false, true)) {
      assertEquals(
          default,
          SettingsRepository.preferencesToAppSettings(emptyPreferences(), default)
              .inAppLoggingEnabled,
      )
      for (saved in listOf(false, true)) {
        val preferences =
            mutablePreferencesOf(SettingsRepository.IN_APP_LOGGING_ENABLED_KEY to saved)
        assertEquals(
            saved,
            SettingsRepository.preferencesToAppSettings(preferences, default).inAppLoggingEnabled,
        )
      }
    }
  }

  @Test
  fun logging_choicePersistsAndPreservesOtherSettings() = runTest {
    val file = File.createTempFile("logging-settings", ".preferences_pb")
    file.deleteOnExit()
    val store = PreferenceDataStoreFactory.create(produceFile = { file })
    val repository = SettingsRepository(store, loggingDefault = false)
    repository.updateShortcutSelection(SHORTCUT)
    repository.setDisclosureAccepted(true)
    repository.enableNotifications()
    repository.setThemeMode(ThemeMode.DARK)
    val before = repository.settings.first()
    repository.setInAppLoggingEnabled(true)
    val reopened = SettingsRepository(store, loggingDefault = false)
    assertEquals(before.copy(inAppLoggingEnabled = true), reopened.settings.first())
    repository.setInAppLoggingEnabled(false)
    assertEquals(before.copy(loggingCleanupPending = true), reopened.settings.first())
    repository.finishLoggingCleanup()
    assertEquals(before, reopened.settings.first())
  }

  private fun createRepository(): SettingsRepository {
    val file = File.createTempFile("settings-repository-test", ".preferences_pb")
    file.deleteOnExit()
    return SettingsRepository(
        PreferenceDataStoreFactory.create(produceFile = { file }),
    )
  }

  private companion object {
    val SHORTCUT =
        ShortcutTarget(
            packageName = "com.example.mail",
            shortcutId = "compose",
            label = "Compose",
            intentUri =
                "intent:#Intent;action=android.intent.action.VIEW;package=com.example.mail;end",
        )
  }
}
