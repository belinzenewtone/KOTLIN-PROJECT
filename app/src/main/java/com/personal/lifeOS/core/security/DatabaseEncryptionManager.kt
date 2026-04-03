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
import java.io.File
import java.security.SecureRandom

/**
 * SQLCipher key and rollout manager.
 *
 * Rollout strategy:
 * - New installs: encrypted DB is enabled by default.
 * - Existing plaintext installs: migrate in place using sqlcipher_export.
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

    fun shouldUseEncryption(
        context: Context,
        dbName: String,
    ): Boolean {
        SQLiteDatabase.loadLibs(context)
        val prefs = prefs(context)
        val mode = prefs.getString(KEY_DB_MODE, null)
        if (mode == MODE_ENCRYPTED) return true
        if (mode == MODE_PLAINTEXT) {
            Log.w(TAG, "Database encryption disabled for this install; staying on plaintext mode.")
            return false
        }
        if (mode == MODE_MIGRATION_FAILED) {
            Log.w(TAG, "Database migration previously failed; staying on plaintext mode.")
            return false
        } else {
            val dbFile = context.getDatabasePath(dbName)
            if (!dbFile.exists()) {
                prefs.edit().putString(KEY_DB_MODE, MODE_ENCRYPTED).apply()
                AppTelemetry.trackEvent(
                    name = "db_encryption_enabled",
                    attributes = mapOf("db_name" to dbName, "reason" to "new_install"),
                )
                return true
            }

            val passphrase = getOrCreatePassphrase(context)
            return if (migratePlaintextToEncrypted(context, dbName, passphrase)) {
                prefs.edit().putString(KEY_DB_MODE, MODE_ENCRYPTED).apply()
                AppTelemetry.trackEvent(
                    name = "db_encryption_enabled",
                    attributes = mapOf("db_name" to dbName, "reason" to "migrated_existing"),
                    captureAsMessage = true,
                )
                true
            } else {
                prefs.edit().putString(KEY_DB_MODE, MODE_PLAINTEXT).apply()
                AppTelemetry.trackEvent(
                    name = "db_encryption_not_applied",
                    attributes = mapOf("db_name" to dbName, "reason" to "migration_failed"),
                    captureAsMessage = true,
                )
                false
            }
        }
    }

    fun createSupportFactory(context: Context): SupportFactory {
        SQLiteDatabase.loadLibs(context)
        val passphrase = SQLiteDatabase.getBytes(getOrCreatePassphrase(context).toCharArray())
        return SupportFactory(passphrase)
    }

    private fun migratePlaintextToEncrypted(
        context: Context,
        dbName: String,
        passphrase: String,
    ): Boolean {
        val dbFile = context.getDatabasePath(dbName)
        val tempFile = context.getDatabasePath("${dbName}_encrypted_tmp")
        val backupFile = context.getDatabasePath("${dbName}_plaintext_backup")
        if (!dbFile.exists()) return true

        deleteIfExists(tempFile)
        deleteIfExists(backupFile)
        deleteSidecars(tempFile)

        val sourcePath = dbFile.absolutePath
        val tempPath = tempFile.absolutePath
        val escapedTempPath = tempPath.replace("'", "''")
        val escapedPassphrase = passphrase.replace("'", "''")

        return runCatching {
            val plaintextDb =
                SQLiteDatabase.openDatabase(
                    sourcePath,
                    "",
                    null,
                    SQLiteDatabase.OPEN_READWRITE,
                )
            plaintextDb.use {
                it.rawExecSQL("PRAGMA journal_mode=DELETE;")
                it.rawExecSQL("ATTACH DATABASE '$escapedTempPath' AS encrypted KEY '$escapedPassphrase';")
                it.rawExecSQL("SELECT sqlcipher_export('encrypted');")
                it.rawExecSQL("DETACH DATABASE encrypted;")
            }

            if (!tempFile.exists() || tempFile.length() == 0L) {
                error("Encrypted export file missing or empty.")
            }

            deleteSidecars(dbFile)
            if (!dbFile.renameTo(backupFile)) {
                error("Could not create plaintext backup before swap.")
            }
            if (!tempFile.renameTo(dbFile)) {
                backupFile.renameTo(dbFile)
                error("Could not replace plaintext DB with encrypted DB.")
            }

            verifyEncryptedDatabase(dbFile, passphrase)
            deleteIfExists(backupFile)
            true
        }.onFailure { error ->
            Log.e(TAG, "Database encryption migration failed.", error)
            AppTelemetry.captureError(
                throwable = error,
                context = mapOf("event" to "db_encryption_migration", "db_name" to dbName),
            )
            restoreBackupIfNeeded(dbFile, backupFile, tempFile)
            prefs(context).edit().putString(KEY_DB_MODE, MODE_MIGRATION_FAILED).apply()
        }.getOrDefault(false)
    }

    private fun verifyEncryptedDatabase(
        dbFile: File,
        passphrase: String,
    ) {
        val verifyDb =
            SQLiteDatabase.openDatabase(
                dbFile.absolutePath,
                passphrase,
                null,
                SQLiteDatabase.OPEN_READONLY,
            )
        verifyDb.use {
            it.rawQuery("SELECT count(*) FROM sqlite_master;", emptyArray()).use { cursor ->
                cursor.moveToFirst()
            }
        }
    }

    private fun restoreBackupIfNeeded(
        dbFile: File,
        backupFile: File,
        tempFile: File,
    ) {
        if (!dbFile.exists() && backupFile.exists()) {
            backupFile.renameTo(dbFile)
        }
        deleteIfExists(tempFile)
        deleteSidecars(tempFile)
    }

    private fun deleteIfExists(file: File) {
        if (file.exists()) {
            file.delete()
        }
    }

    private fun deleteSidecars(file: File) {
        val wal = File("${file.absolutePath}-wal")
        val shm = File("${file.absolutePath}-shm")
        val journal = File("${file.absolutePath}-journal")
        deleteIfExists(wal)
        deleteIfExists(shm)
        deleteIfExists(journal)
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
