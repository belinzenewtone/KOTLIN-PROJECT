package com.personal.lifeOS.core.security

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.personal.lifeOS.core.observability.AppTelemetry
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SupportFactory
import java.security.SecureRandom

/**
 * SQLCipher key and rollout manager.
 *
 * Rollout strategy:
 * - New installs: encrypted DB is enabled by default.
 * - Existing installs: remain plaintext until explicit migration is implemented.
 *
 * This avoids accidental lockout/data-loss for users with existing plaintext DB files.
 */
object DatabaseEncryptionManager {
    private const val TAG = "DbEncryptionManager"
    private const val PREF_FILE = "db_encryption_prefs"
    private const val KEY_DB_MODE = "db_mode"
    private const val KEY_DB_PASSPHRASE = "db_passphrase"
    private const val MODE_ENCRYPTED = "encrypted"
    private const val MODE_PLAINTEXT = "plaintext"

    fun shouldUseEncryption(
        context: Context,
        dbName: String,
    ): Boolean {
        val prefs = prefs(context)
        val mode = prefs.getString(KEY_DB_MODE, null)
        if (mode == MODE_ENCRYPTED) return true
        if (mode == MODE_PLAINTEXT) return false

        val dbExists = context.getDatabasePath(dbName).exists()
        return if (dbExists) {
            prefs.edit().putString(KEY_DB_MODE, MODE_PLAINTEXT).apply()
            AppTelemetry.trackEvent(
                name = "db_encryption_pending_migration",
                attributes = mapOf("db_name" to dbName),
                captureAsMessage = true,
            )
            Log.w(TAG, "Existing database detected; keeping plaintext mode pending migration.")
            false
        } else {
            prefs.edit().putString(KEY_DB_MODE, MODE_ENCRYPTED).apply()
            AppTelemetry.trackEvent(
                name = "db_encryption_enabled",
                attributes = mapOf("db_name" to dbName, "reason" to "new_install"),
            )
            true
        }
    }

    fun createSupportFactory(context: Context): SupportFactory {
        SQLiteDatabase.loadLibs(context)
        val passphrase = SQLiteDatabase.getBytes(getOrCreatePassphrase(context).toCharArray())
        return SupportFactory(passphrase)
    }

    private fun getOrCreatePassphrase(context: Context): String {
        val prefs = prefs(context)
        val existing = prefs.getString(KEY_DB_PASSPHRASE, null)
        if (!existing.isNullOrBlank()) return existing

        val raw = ByteArray(32)
        SecureRandom().nextBytes(raw)
        val encoded = Base64.encodeToString(raw, Base64.NO_WRAP)
        prefs.edit().putString(KEY_DB_PASSPHRASE, encoded).apply()
        return encoded
    }

    private fun prefs(context: Context): SharedPreferences {
        return runCatching {
            val masterKey =
                MasterKey
                    .Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build()
            EncryptedSharedPreferences.create(
                context,
                PREF_FILE,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
            )
        }.getOrElse {
            Log.e(TAG, "EncryptedSharedPreferences unavailable; falling back to app-private prefs.", it)
            context.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE)
        }
    }
}
