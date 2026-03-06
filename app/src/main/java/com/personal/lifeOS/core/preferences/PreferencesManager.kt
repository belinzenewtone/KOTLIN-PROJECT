package com.personal.lifeOS.core.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PreferencesManager @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    companion object {
        val KEY_FIRST_LAUNCH = booleanPreferencesKey("first_launch")
        val KEY_PROFILE_CREATED_AT = longPreferencesKey("profile_created_at")
        val KEY_PROFILE_PIC_URI = stringPreferencesKey("profile_pic_uri")
        val KEY_SMS_IMPORTED = booleanPreferencesKey("sms_imported")
        val KEY_APP_LOCKED = booleanPreferencesKey("app_locked")
    }

    val isFirstLaunch: Flow<Boolean> = dataStore.data.map { it[KEY_FIRST_LAUNCH] ?: true }
    val profileCreatedAt: Flow<Long> = dataStore.data.map { it[KEY_PROFILE_CREATED_AT] ?: 0L }
    val profilePicUri: Flow<String> = dataStore.data.map { it[KEY_PROFILE_PIC_URI] ?: "" }
    val smsImported: Flow<Boolean> = dataStore.data.map { it[KEY_SMS_IMPORTED] ?: false }

    suspend fun setFirstLaunchComplete() {
        dataStore.edit { it[KEY_FIRST_LAUNCH] = false }
    }

    suspend fun setProfileCreatedAt(timestamp: Long) {
        dataStore.edit { it[KEY_PROFILE_CREATED_AT] = timestamp }
    }

    suspend fun setProfilePicUri(uri: String) {
        dataStore.edit { it[KEY_PROFILE_PIC_URI] = uri }
    }

    suspend fun setSmsImported() {
        dataStore.edit { it[KEY_SMS_IMPORTED] = true }
    }
}
