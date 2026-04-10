package com.personal.lifeOS.core.security

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthSessionStore
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        companion object {
            private const val PREFS_NAME = "lifeos_secure_session"
            private const val KEY_ACCESS_TOKEN = "access_token"
            private const val KEY_USER_ID = "user_id"
        }

        private val prefs: SharedPreferences by lazy { createSecurePrefs() }

        fun saveSession(
            accessToken: String,
            userId: String,
        ) {
            prefs.edit {
                putString(KEY_ACCESS_TOKEN, accessToken)
                putString(KEY_USER_ID, userId)
            }
        }

        fun getAccessToken(): String {
            return prefs.getString(KEY_ACCESS_TOKEN, "").orEmpty()
        }

        fun getUserId(): String {
            return prefs.getString(KEY_USER_ID, "").orEmpty()
        }

        fun clearSession() {
            prefs.edit {
                remove(KEY_ACCESS_TOKEN)
                remove(KEY_USER_ID)
            }
        }

        private fun createSecurePrefs(): SharedPreferences {
            return try {
                val masterKey =
                    MasterKey.Builder(context)
                        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                        .build()

                EncryptedSharedPreferences.create(
                    context,
                    PREFS_NAME,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
                )
            } catch (_: Exception) {
                // Keep auth working on devices that cannot initialize encrypted storage.
                context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            }
        }
    }
