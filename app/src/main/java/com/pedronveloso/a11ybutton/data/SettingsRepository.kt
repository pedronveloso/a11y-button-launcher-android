/*
 * Copyright (C) 2026 Pedro Veloso
 * SPDX-License-Identifier: Apache-2.0
 */
package com.pedronveloso.a11ybutton.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.pedronveloso.a11ybutton.model.AppSettings
import com.pedronveloso.a11ybutton.model.NotificationPreference
import com.pedronveloso.a11ybutton.model.ShortcutTarget
import com.pedronveloso.a11ybutton.model.ThemeMode
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import timber.log.Timber

private const val SETTINGS_DATASTORE_NAME = "app_settings"

private val Context.dataStore: DataStore<Preferences> by
    preferencesDataStore(
        name = SETTINGS_DATASTORE_NAME,
    )

class SettingsRepository(
    private val dataStore: DataStore<Preferences>,
    private val loggingDefault: Boolean = com.pedronveloso.a11ybutton.BuildConfig.DEBUG,
) {
  val settings: Flow<AppSettings> =
      dataStore.data
          .catch { throwable ->
            if (throwable is IOException) {
              Timber.w(throwable, "Failed to read app settings; falling back to defaults")
              emit(emptyPreferences())
            } else {
              throw throwable
            }
          }
          .map { preferencesToAppSettings(it, loggingDefault) }

  suspend fun setInAppLoggingEnabled(enabled: Boolean) {
    Timber.i("Updating in-app logging to %s", enabled)
    dataStore.edit { preferences ->
      preferences[IN_APP_LOGGING_ENABLED_KEY] = enabled
      if (!enabled) preferences[LOGGING_CLEANUP_PENDING_KEY] = true
    }
  }

  internal suspend fun finishLoggingCleanup() {
    Timber.i("Finished clearing retained logs")
    dataStore.edit { it.remove(LOGGING_CLEANUP_PENDING_KEY) }
  }

  suspend fun setDisclosureAccepted(accepted: Boolean) {
    Timber.i("Updating disclosure acceptance to %s", accepted)
    dataStore.edit { preferences -> preferences[DISCLOSURE_ACCEPTED_KEY] = accepted }
  }

  suspend fun setOnboardingCompleted(completed: Boolean) {
    Timber.i("Updating onboarding completion to %s", completed)
    dataStore.edit { preferences -> preferences[ONBOARDING_COMPLETED_KEY] = completed }
  }

  suspend fun enableNotifications() {
    setNotificationPreference(NotificationPreference.Enabled)
  }

  suspend fun disableNotifications() {
    setNotificationPreference(NotificationPreference.Disabled)
  }

  suspend fun optOutNotifications() {
    setNotificationPreference(NotificationPreference.OptedOut)
  }

  suspend fun setXiaomiRecentsLockConfirmed(confirmed: Boolean) {
    Timber.i("Updating Xiaomi recents lock confirmation to %s", confirmed)
    dataStore.edit { preferences -> preferences[XIAOMI_RECENTS_LOCK_CONFIRMED_KEY] = confirmed }
  }

  suspend fun setThemeMode(mode: ThemeMode) {
    Timber.i("Updating theme mode to %s", mode)
    dataStore.edit { preferences -> preferences[THEME_MODE_KEY] = mode.name }
  }

  suspend fun updateSelection(
      packageName: String?,
      componentName: String?,
  ) {
    Timber.i(
        "Updating selected app to package=%s component=%s",
        packageName,
        componentName,
    )
    dataStore.edit { preferences ->
      preferences[SELECTED_PACKAGE_NAME_KEY] = packageName.orEmpty()
      preferences[SELECTED_COMPONENT_NAME_KEY] = componentName.orEmpty()
      preferences.clearShortcut()
    }
  }

  /** Selects a shortcut as the button's target, replacing any selected app. */
  suspend fun updateShortcutSelection(shortcut: ShortcutTarget) {
    Timber.i(
        "Updating selected shortcut to package=%s id=%s",
        shortcut.packageName,
        shortcut.shortcutId,
    )
    dataStore.edit { preferences ->
      preferences[SELECTED_PACKAGE_NAME_KEY] = ""
      preferences[SELECTED_COMPONENT_NAME_KEY] = ""
      preferences[SELECTED_SHORTCUT_PACKAGE_KEY] = shortcut.packageName
      preferences[SELECTED_SHORTCUT_ID_KEY] = shortcut.shortcutId.orEmpty()
      preferences[SELECTED_SHORTCUT_LABEL_KEY] = shortcut.label
      preferences[SELECTED_SHORTCUT_INTENT_URI_KEY] = shortcut.intentUri
    }
  }

  /** Clears whichever target is selected, app or shortcut. */
  suspend fun clearSelection() {
    Timber.i("Clearing selected target")
    updateSelection(packageName = null, componentName = null)
  }

  companion object {
    internal val SELECTED_PACKAGE_NAME_KEY = stringPreferencesKey("selected_package_name")
    internal val SELECTED_COMPONENT_NAME_KEY = stringPreferencesKey("selected_component_name")
    internal val SELECTED_SHORTCUT_PACKAGE_KEY = stringPreferencesKey("selected_shortcut_package")
    internal val SELECTED_SHORTCUT_ID_KEY = stringPreferencesKey("selected_shortcut_id")
    internal val SELECTED_SHORTCUT_LABEL_KEY = stringPreferencesKey("selected_shortcut_label")
    internal val SELECTED_SHORTCUT_INTENT_URI_KEY =
        stringPreferencesKey("selected_shortcut_intent_uri")
    internal val DISCLOSURE_ACCEPTED_KEY = booleanPreferencesKey("disclosure_accepted")
    internal val XIAOMI_RECENTS_LOCK_CONFIRMED_KEY =
        booleanPreferencesKey("xiaomi_recents_lock_confirmed")
    internal val NOTIFICATION_PREFERENCE_KEY = stringPreferencesKey("notification_preference")
    internal val NOTIFICATIONS_OPTED_OUT_KEY = booleanPreferencesKey("notifications_opted_out")
    internal val NOTIFICATIONS_ENABLED_KEY = booleanPreferencesKey("notifications_enabled")
    internal val LOGGING_CLEANUP_PENDING_KEY = booleanPreferencesKey("logging_cleanup_pending")
    internal val IN_APP_LOGGING_ENABLED_KEY = booleanPreferencesKey("in_app_logging_enabled")
    internal val THEME_MODE_KEY = stringPreferencesKey("theme_mode")
    internal val ONBOARDING_COMPLETED_KEY = booleanPreferencesKey("onboarding_completed")

    fun fromContext(context: Context): SettingsRepository = SettingsRepository(context.dataStore)

    internal fun preferencesToAppSettings(
        preferences: Preferences,
        loggingDefault: Boolean = com.pedronveloso.a11ybutton.BuildConfig.DEBUG,
    ): AppSettings =
        AppSettings(
            loggingCleanupPending = preferences[LOGGING_CLEANUP_PENDING_KEY] ?: false,
            inAppLoggingEnabled = preferences[IN_APP_LOGGING_ENABLED_KEY] ?: loggingDefault,
            selectedPackageName = preferences[SELECTED_PACKAGE_NAME_KEY].nullIfBlank(),
            selectedComponentName = preferences[SELECTED_COMPONENT_NAME_KEY].nullIfBlank(),
            selectedShortcut = preferences.selectedShortcut(),
            disclosureAccepted = preferences[DISCLOSURE_ACCEPTED_KEY] ?: false,
            xiaomiRecentsLockConfirmed = preferences[XIAOMI_RECENTS_LOCK_CONFIRMED_KEY] ?: false,
            notificationPreference = preferences.notificationPreference(),
            themeMode =
                ThemeMode.entries.find { it.name == preferences[THEME_MODE_KEY] }
                    ?: ThemeMode.SYSTEM,
            // People who accepted the disclosure before onboarding existed have already set up.
            onboardingCompleted =
                preferences[ONBOARDING_COMPLETED_KEY]
                    ?: (preferences[DISCLOSURE_ACCEPTED_KEY] ?: false),
        )
  }

  private suspend fun setNotificationPreference(preference: NotificationPreference) {
    Timber.i("Updating notification preference to %s", preference)
    dataStore.edit { preferences ->
      preferences[NOTIFICATION_PREFERENCE_KEY] = preference.name
      preferences[NOTIFICATIONS_ENABLED_KEY] = preference == NotificationPreference.Enabled
      preferences[NOTIFICATIONS_OPTED_OUT_KEY] = preference == NotificationPreference.OptedOut
    }
  }
}

private fun String?.nullIfBlank(): String? = if (isNullOrBlank()) null else this

private fun MutablePreferences.clearShortcut() {
  remove(SettingsRepository.SELECTED_SHORTCUT_PACKAGE_KEY)
  remove(SettingsRepository.SELECTED_SHORTCUT_ID_KEY)
  remove(SettingsRepository.SELECTED_SHORTCUT_LABEL_KEY)
  remove(SettingsRepository.SELECTED_SHORTCUT_INTENT_URI_KEY)
}

private fun Preferences.selectedShortcut(): ShortcutTarget? {
  val packageName = this[SettingsRepository.SELECTED_SHORTCUT_PACKAGE_KEY].nullIfBlank()
  val intentUri = this[SettingsRepository.SELECTED_SHORTCUT_INTENT_URI_KEY].nullIfBlank()
  val label = this[SettingsRepository.SELECTED_SHORTCUT_LABEL_KEY].nullIfBlank()
  if (packageName == null || intentUri == null || label == null) return null
  return ShortcutTarget(
      packageName = packageName,
      shortcutId = this[SettingsRepository.SELECTED_SHORTCUT_ID_KEY].nullIfBlank(),
      label = label,
      intentUri = intentUri,
  )
}

private fun Preferences.notificationPreference(): NotificationPreference {
  val storedPreference =
      this[SettingsRepository.NOTIFICATION_PREFERENCE_KEY]?.let { savedValue ->
        NotificationPreference.entries.find { it.name == savedValue }
      }
  if (storedPreference != null) {
    return storedPreference
  }

  return when {
    this[SettingsRepository.NOTIFICATIONS_OPTED_OUT_KEY] == true -> NotificationPreference.OptedOut
    this[SettingsRepository.NOTIFICATIONS_ENABLED_KEY] == true -> NotificationPreference.Enabled
    else -> NotificationPreference.Disabled
  }
}
