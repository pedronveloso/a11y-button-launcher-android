/*
 * Copyright (C) 2026 Pedro Veloso
 * SPDX-License-Identifier: Apache-2.0
 */
package com.pedronveloso.a11ybutton.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.pedronveloso.a11ybutton.model.AppSettings
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
          .map(::preferencesToAppSettings)

  suspend fun setDisclosureAccepted(accepted: Boolean) {
    Timber.i("Updating disclosure acceptance to %s", accepted)
    dataStore.edit { preferences -> preferences[DISCLOSURE_ACCEPTED_KEY] = accepted }
  }

  suspend fun setNotificationsEnabled(enabled: Boolean) {
    Timber.i("Updating notifications enabled to %s", enabled)
    dataStore.edit { preferences -> preferences[NOTIFICATIONS_ENABLED_KEY] = enabled }
  }

  suspend fun setNotificationsOptedOut(optedOut: Boolean) {
    Timber.i("Updating notifications opted out to %s", optedOut)
    dataStore.edit { preferences -> preferences[NOTIFICATIONS_OPTED_OUT_KEY] = optedOut }
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
    }
  }

  companion object {
    internal val SELECTED_PACKAGE_NAME_KEY = stringPreferencesKey("selected_package_name")
    internal val SELECTED_COMPONENT_NAME_KEY = stringPreferencesKey("selected_component_name")
    internal val DISCLOSURE_ACCEPTED_KEY = booleanPreferencesKey("disclosure_accepted")
    internal val XIAOMI_RECENTS_LOCK_CONFIRMED_KEY =
        booleanPreferencesKey("xiaomi_recents_lock_confirmed")
    internal val NOTIFICATIONS_OPTED_OUT_KEY = booleanPreferencesKey("notifications_opted_out")
    internal val NOTIFICATIONS_ENABLED_KEY = booleanPreferencesKey("notifications_enabled")
    internal val THEME_MODE_KEY = stringPreferencesKey("theme_mode")

    fun fromContext(context: Context): SettingsRepository = SettingsRepository(context.dataStore)

    internal fun preferencesToAppSettings(preferences: Preferences): AppSettings =
        AppSettings(
            selectedPackageName = preferences[SELECTED_PACKAGE_NAME_KEY].nullIfBlank(),
            selectedComponentName = preferences[SELECTED_COMPONENT_NAME_KEY].nullIfBlank(),
            disclosureAccepted = preferences[DISCLOSURE_ACCEPTED_KEY] ?: false,
            xiaomiRecentsLockConfirmed = preferences[XIAOMI_RECENTS_LOCK_CONFIRMED_KEY] ?: false,
            notificationsOptedOut = preferences[NOTIFICATIONS_OPTED_OUT_KEY] ?: false,
            notificationsEnabled = preferences[NOTIFICATIONS_ENABLED_KEY] ?: false,
            themeMode =
                ThemeMode.entries.find { it.name == preferences[THEME_MODE_KEY] }
                    ?: ThemeMode.SYSTEM,
        )
  }
}

private fun String?.nullIfBlank(): String? = if (isNullOrBlank()) null else this
