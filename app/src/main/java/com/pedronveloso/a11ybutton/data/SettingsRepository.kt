/*
 * Copyright (C) 2026 Pedro Veloso
 * All rights reserved.
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
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private const val SETTINGS_DATASTORE_NAME = "app_settings"

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = SETTINGS_DATASTORE_NAME,
)

class SettingsRepository(
    private val dataStore: DataStore<Preferences>,
) {
    val settings: Flow<AppSettings> =
        dataStore.data
            .catch { throwable ->
                if (throwable is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw throwable
                }
            }
            .map(::preferencesToAppSettings)

    suspend fun setDisclosureAccepted(accepted: Boolean) {
        dataStore.edit { preferences ->
            preferences[DISCLOSURE_ACCEPTED_KEY] = accepted
        }
    }

    suspend fun updateSelection(
        packageName: String?,
        componentName: String?,
    ) {
        dataStore.edit { preferences ->
            preferences[SELECTED_PACKAGE_NAME_KEY] = packageName.orEmpty()
            preferences[SELECTED_COMPONENT_NAME_KEY] = componentName.orEmpty()
        }
    }

    companion object {
        internal val SELECTED_PACKAGE_NAME_KEY = stringPreferencesKey("selected_package_name")
        internal val SELECTED_COMPONENT_NAME_KEY =
            stringPreferencesKey("selected_component_name")
        internal val DISCLOSURE_ACCEPTED_KEY = booleanPreferencesKey("disclosure_accepted")

        fun fromContext(context: Context): SettingsRepository = SettingsRepository(context.dataStore)

        internal fun preferencesToAppSettings(preferences: Preferences): AppSettings =
            AppSettings(
                selectedPackageName = preferences[SELECTED_PACKAGE_NAME_KEY].nullIfBlank(),
                selectedComponentName = preferences[SELECTED_COMPONENT_NAME_KEY].nullIfBlank(),
                disclosureAccepted = preferences[DISCLOSURE_ACCEPTED_KEY] ?: false,
            )
    }
}

private fun String?.nullIfBlank(): String? = if (isNullOrBlank()) null else this
