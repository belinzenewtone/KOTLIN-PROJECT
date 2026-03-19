package com.personal.lifeOS.features.export.data.repository

import android.content.Context
import android.os.Environment
import com.personal.lifeOS.features.export.domain.model.ExportFormat
import com.personal.lifeOS.features.export.domain.model.ExportRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.security.SecureRandom
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.text.Charsets.UTF_8

@Singleton
class ExportFileWriter
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        fun encodeForStorage(
            bytes: ByteArray,
            passphrase: String?,
        ): Pair<ByteArray, Boolean> {
            val encrypted = !passphrase.isNullOrBlank()
            return if (encrypted) {
                encryptBytes(bytes, passphrase.orEmpty()) to true
            } else {
                bytes to false
            }
        }

        fun writeFile(
            request: ExportRequest,
            bytes: ByteArray,
        ): File {
            val directory = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: context.filesDir
            if (!directory.exists()) directory.mkdirs()

            val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
            val baseName =
                "personalos_${request.domain.name.lowercase(Locale.US)}_" +
                    "${request.format.name.lowercase(Locale.US)}_$timestamp"
            val encryptedSuffix = if (request.encryptionPassphrase.isNullOrBlank()) "" else ".enc"
            val extension = request.format.extension()
            val file = File(directory, "$baseName.$extension$encryptedSuffix")
            file.writeBytes(bytes)
            return file
        }

        private fun encryptBytes(
            plainBytes: ByteArray,
            passphrase: String,
        ): ByteArray {
            require(passphrase.isNotBlank()) { "Encryption passphrase cannot be blank." }

            val random = SecureRandom()
            val salt = ByteArray(16).also(random::nextBytes)
            val iv = ByteArray(12).also(random::nextBytes)

            val secretFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            val keySpec = PBEKeySpec(passphrase.toCharArray(), salt, 120_000, 256)
            val keyBytes = secretFactory.generateSecret(keySpec).encoded
            val secretKey = SecretKeySpec(keyBytes, "AES")

            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(128, iv))
            val encrypted = cipher.doFinal(plainBytes)

            val magic = "LIFEOS1".toByteArray(UTF_8)
            return magic + salt + iv + encrypted
        }
    }

private fun ExportFormat.extension(): String {
    return when (this) {
        ExportFormat.JSON -> "json"
        ExportFormat.CSV -> "csv"
    }
}
