package com.personal.lifeOS.core.security

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import android.util.Log
import androidx.core.content.edit
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
 * - Existing plaintext installs: stay on plaintext mode until migration rollout is re-enabled.
 * - If migration fails: keep plaintext mode to avoid data-loss/lockout.
 */
object DatabaseEncryptionManager {
    private const val TAG = "DbEncryptionManager"
    private const val PREF_FILE = "db_encryption_prefs"
    private const val KEY_DB_MODE = "db_mode"
    private const val KEY_DB_PASSPHRASE = "db_passphrase"
    private const val MODE_ENCRYPTED = "encrypted"
    private const val MODE_PLAINTEXT = "plaintext"
    private const val MODE_MIGRATION_FAILED = "migration_failed"
    private const val EMERGENCY_DISABLE_ENCRYPTION = true

    fun shouldUseEncryption(
        context: Context,
        dbName: String,
    ): Boolean {
        if (EMERGENCY_DISABLE_ENCRYPTION) {
            runCatching { prefs(context).edit { putString(KEY_DB_MODE, MODE_PLAINTEXT) } }
            AppTelemetry.trackEvent(
                name = "db_encryption_not_applied",
                attributes = mapOf("db_name" to dbName, "reason" to "emergency_disable_encryption"),
                captureAsMessage = true,
            )
            return false
        }
        return runCatching {
            SQLiteDatabase.loadLibs(context)
            val prefs = prefs(context)
            when (prefs.getString(KEY_DB_MODE, null)) {
                MODE_ENCRYPTED -> true
                MODE_PLAINTEXT,
                MODE_MIGRATION_FAILED,
                -> {
                    Log.w(TAG, "Database encryption disabled for this install; staying on plaintext mode.")
                    false
                }
                else -> {
                    val dbFile = context.getDatabasePath(dbName)
                    if (!dbFile.exists()) {
                        prefs.edit { putString(KEY_DB_MODE, MODE_ENCRYPTED) }
                        AppTelemetry.trackEvent(
                            name = "db_encryption_enabled",
                            attributes = mapOf("db_name" to dbName, "reason" to "new_install"),
                        )
                        true
                    } else {
                        // Safety-first hotfix: do not attempt in-place migration during app start.
                        // Existing installs continue on plaintext mode until explicit migration rollout.
                        prefs.edit { putString(KEY_DB_MODE, MODE_PLAINTEXT) }
                        AppTelemetry.trackEvent(
                            name = "db_encryption_not_applied",
                            attributes = mapOf("db_name" to dbName, "reason" to "existing_install_fallback"),
                            captureAsMessage = true,
                        )
                        false
                    }
                }
            }
        }.getOrElse { error ->
            Log.e(TAG, "Failed to resolve database encryption mode. Falling back to plaintext.", error)
            AppTelemetry.captureError(
                throwable = error,
                context = mapOf("event" to "db_encryption_mode_resolve", "db_name" to dbName),
            )
            runCatching {
                prefs(context).edit { putString(KEY_DB_MODE, MODE_PLAINTEXT) }
            }
            false
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
        prefs.edit { putString(KEY_DB_PASSPHRASE, encoded) }
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
