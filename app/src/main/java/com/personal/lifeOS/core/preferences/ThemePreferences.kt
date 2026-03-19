package com.personal.lifeOS.core.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.personal.lifeOS.ui.theme.AppThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ThemePreferences
    @Inject
    constructor(
        private val dataStore: DataStore<Preferences>,
    ) {
        fun themeModeFlow(): Flow<AppThemeMode> {
            return dataStore.data.map { prefs ->
                val raw = prefs[KEY_THEME_MODE].orEmpty()
                runCatching { AppThemeMode.valueOf(raw) }.getOrDefault(AppThemeMode.SYSTEM)
            }
        }

        suspend fun setThemeMode(mode: AppThemeMode) {
            dataStore.edit { prefs ->
                prefs[KEY_THEME_MODE] = mode.name
            }
        }

        companion object {
            val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        }
    }
