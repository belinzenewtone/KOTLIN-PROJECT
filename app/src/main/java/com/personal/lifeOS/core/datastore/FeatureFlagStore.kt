package com.personal.lifeOS.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FeatureFlagStore
    @Inject
    constructor(
        private val dataStore: DataStore<Preferences>,
    ) {
        fun observe(flag: FeatureFlag): Flow<Boolean> {
            val key = flagKey(flag)
            return dataStore.data.map { preferences ->
                preferences[key] ?: flag.defaultEnabled
            }
        }

        suspend fun isEnabled(flag: FeatureFlag): Boolean {
            val key = flagKey(flag)
            return dataStore.data.first()[key] ?: flag.defaultEnabled
        }

        suspend fun applyRemoteValues(values: Map<String, Boolean>) {
            if (values.isEmpty()) return
            dataStore.edit { preferences ->
                values.forEach { (rawKey, enabled) ->
                    FeatureFlag.fromKey(rawKey)?.let { flag ->
                        preferences[flagKey(flag)] = enabled
                    }
                }
            }
        }

        suspend fun snapshot(): Map<FeatureFlag, Boolean> {
            val prefs = dataStore.data.first()
            return FeatureFlag.entries.associateWith { flag ->
                prefs[flagKey(flag)] ?: flag.defaultEnabled
            }
        }

        private fun flagKey(flag: FeatureFlag) = booleanPreferencesKey("feature_flag_${flag.key}")
    }
