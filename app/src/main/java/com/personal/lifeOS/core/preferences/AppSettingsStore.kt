package com.personal.lifeOS.core.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.appSettingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "lifeos_preferences")

object AppSettingsKeys {
    val BIOMETRIC_ENABLED = booleanPreferencesKey("biometric_enabled")
    val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
}

@Singleton
class AppSettingsStore
    @Inject
    constructor(
        private val dataStore: DataStore<Preferences>,
    ) {
        fun biometricEnabledFlow(): Flow<Boolean> {
            return dataStore.data.map { prefs ->
                prefs[AppSettingsKeys.BIOMETRIC_ENABLED] ?: false
            }
        }

        suspend fun isBiometricEnabled(): Boolean {
            return dataStore.data.first()[AppSettingsKeys.BIOMETRIC_ENABLED] ?: false
        }

        suspend fun setBiometricEnabled(enabled: Boolean) {
            dataStore.edit { prefs ->
                prefs[AppSettingsKeys.BIOMETRIC_ENABLED] = enabled
            }
        }

        fun notificationsEnabledFlow(): Flow<Boolean> {
            return dataStore.data.map { prefs ->
                prefs[AppSettingsKeys.NOTIFICATIONS_ENABLED] ?: true
            }
        }

        suspend fun areNotificationsEnabled(): Boolean {
            return dataStore.data.first()[AppSettingsKeys.NOTIFICATIONS_ENABLED] ?: true
        }

        suspend fun setNotificationsEnabled(enabled: Boolean) {
            dataStore.edit { prefs ->
                prefs[AppSettingsKeys.NOTIFICATIONS_ENABLED] = enabled
            }
        }

        companion object {
            suspend fun areNotificationsEnabled(context: Context): Boolean {
                return context.appSettingsDataStore.data.first()[AppSettingsKeys.NOTIFICATIONS_ENABLED] ?: true
            }
        }
    }
