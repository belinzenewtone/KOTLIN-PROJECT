package com.personal.lifeOS.core.update

import android.app.Activity
import android.app.DownloadManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Environment
import android.provider.Settings
import androidx.core.content.pm.PackageInfoCompat
import com.google.gson.FieldNamingPolicy
import com.google.gson.GsonBuilder
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

data class OtaUpdateManifest(
    val versionCode: Long,
    val versionName: String? = null,
    val apkUrl: String,
    val apkSha256: String? = null,
    val changelog: String? = null,
    val mandatory: Boolean = false,
    val title: String? = null,
    val message: String? = null,
    val websiteUrl: String? = null,
)

sealed interface OtaCheckResult {
    data class UpdateAvailable(val manifest: OtaUpdateManifest) : OtaCheckResult

    data object UpToDate : OtaCheckResult

    data object NotConfigured : OtaCheckResult

    data class Error(val message: String) : OtaCheckResult
}

sealed interface OtaDownloadResult {
    data class Success(val apkUri: Uri) : OtaDownloadResult

    data object Cancelled : OtaDownloadResult

    data class Error(val message: String) : OtaDownloadResult
}

sealed interface OtaInstallResult {
    data object Started : OtaInstallResult

    data object RequiresUnknownSourcesPermission : OtaInstallResult

    data class Error(val message: String) : OtaInstallResult
}

data class OtaDownloadProgress(
    val progressPercent: Int?,
    val downloadedBytes: Long,
    val totalBytes: Long?,
    val bytesPerSecond: Long?,
)

/**
 * APK OTA update manager for non-Play distribution.
 */
object OtaUpdateManager {
    private val gson =
        GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .create()
    private val httpClient =
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

    suspend fun checkForUpdate(
        context: Context,
        manifestUrl: String,
    ): OtaCheckResult =
        withContext(Dispatchers.IO) {
            if (manifestUrl.isBlank()) return@withContext OtaCheckResult.NotConfigured

            runCatching {
                val request = Request.Builder().url(manifestUrl).get().build()
                val body =
                    httpClient.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) {
                            return@use null
                        }
                        response.body?.string()
                    }
                if (body.isNullOrBlank()) {
                    return@runCatching OtaCheckResult.Error("Empty OTA manifest response.")
                }

                val manifest = gson.fromJson(body, OtaUpdateManifest::class.java)
                if (manifest.apkUrl.isBlank() || manifest.versionCode <= 0L) {
                    return@runCatching OtaCheckResult.Error("Invalid OTA manifest fields.")
                }

                val currentVersionCode = getCurrentVersionCode(context)
                if (manifest.versionCode > currentVersionCode) {
                    OtaCheckResult.UpdateAvailable(manifest)
                } else {
                    OtaCheckResult.UpToDate
                }
            }.getOrElse { OtaCheckResult.Error(it.message ?: "Failed to check for updates.") }
        }

    suspend fun downloadUpdate(
        context: Context,
        manifest: OtaUpdateManifest,
        onProgress: (Int?) -> Unit = {},
        onProgressDetails: (OtaDownloadProgress) -> Unit = {},
        onEnqueued: (Long) -> Unit = {},
    ): OtaDownloadResult =
        withContext(Dispatchers.IO) {
            val manager =
                context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager
                    ?: return@withContext OtaDownloadResult.Error("Download manager unavailable.")

            val request =
                DownloadManager.Request(Uri.parse(manifest.apkUrl))
                    .setMimeType("application/vnd.android.package-archive")
                    .setTitle("BELTECH update")
                    .setDescription("Downloading version ${manifest.versionName ?: manifest.versionCode}")
                    .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                    .setAllowedOverMetered(true)
                    .setAllowedOverRoaming(true)
                    .setDestinationInExternalFilesDir(
                        context,
                        Environment.DIRECTORY_DOWNLOADS,
                        "beltech_update_v${manifest.versionCode}.apk",
                    )

            val downloadId =
                runCatching { manager.enqueue(request) }
                    .getOrElse { return@withContext OtaDownloadResult.Error(it.message ?: "Failed to start download.") }
            onEnqueued(downloadId)

            try {
                waitForDownloadCompletion(
                    context = context,
                    manager = manager,
                    downloadId = downloadId,
                    expectedSha256 = manifest.apkSha256,
                    onProgress = onProgress,
                    onProgressDetails = onProgressDetails,
                )
            } catch (_: CancellationException) {
                manager.remove(downloadId)
                OtaDownloadResult.Cancelled
            }
        }

    fun launchInstaller(
        activity: Activity,
        apkUri: Uri,
    ): OtaInstallResult {
        if (!activity.packageManager.canRequestPackageInstalls()) {
            val settingsIntent =
                Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    data = Uri.parse("package:${activity.packageName}")
                }
            activity.startActivity(settingsIntent)
            return OtaInstallResult.RequiresUnknownSourcesPermission
        }

        val installIntent =
            Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

        return try {
            activity.startActivity(installIntent)
            OtaInstallResult.Started
        } catch (e: ActivityNotFoundException) {
            OtaInstallResult.Error(e.message ?: "No installer available on device.")
        } catch (e: SecurityException) {
            OtaInstallResult.Error(e.message ?: "Installer launch denied.")
        }
    }

    fun cancelDownload(
        context: Context,
        downloadId: Long,
    ): Boolean {
        val manager =
            context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager
                ?: return false
        return runCatching { manager.remove(downloadId) > 0L }.getOrDefault(false)
    }

    fun openWebsite(
        context: Context,
        url: String,
    ): Boolean {
        val target = url.trim()
        if (target.isBlank()) return false
        val intent =
            Intent(Intent.ACTION_VIEW, Uri.parse(target)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        return try {
            context.startActivity(intent)
            true
        } catch (_: ActivityNotFoundException) {
            false
        } catch (_: SecurityException) {
            false
        }
    }

    private fun getCurrentVersionCode(context: Context): Long {
        val packageInfo =
            context.packageManager.getPackageInfo(context.packageName, 0)
        return PackageInfoCompat.getLongVersionCode(packageInfo)
    }

    private suspend fun waitForDownloadCompletion(
        context: Context,
        manager: DownloadManager,
        downloadId: Long,
        expectedSha256: String?,
        onProgress: (Int?) -> Unit,
        onProgressDetails: (OtaDownloadProgress) -> Unit,
    ): OtaDownloadResult {
        val query = DownloadManager.Query().setFilterById(downloadId)
        var lastSampleTimeMs = System.currentTimeMillis()
        var lastDownloadedBytes = 0L
        while (true) {
            val snapshot =
                readDownloadSnapshot(manager, query)
                    ?: return OtaDownloadResult.Error("Download entry not found.")
            onProgress(snapshot.progress)
            onProgressDetails(snapshot.toProgressSample(lastSampleTimeMs, lastDownloadedBytes))
            lastSampleTimeMs = System.currentTimeMillis()
            lastDownloadedBytes = snapshot.downloadedBytes.coerceAtLeast(0L)

            when (snapshot.status) {
                DownloadManager.STATUS_SUCCESSFUL ->
                    return verifyAndBuildSuccessResult(
                        context = context,
                        manager = manager,
                        downloadId = downloadId,
                        expectedSha256 = expectedSha256,
                    )

                DownloadManager.STATUS_FAILED ->
                    return OtaDownloadResult.Error("Download failed (reason ${snapshot.reason ?: -1}).")

                DownloadManager.STATUS_PAUSED,
                DownloadManager.STATUS_PENDING,
                DownloadManager.STATUS_RUNNING,
                -> Unit
            }
            delay(750L)
        }
    }

    private fun readDownloadSnapshot(
        manager: DownloadManager,
        query: DownloadManager.Query,
    ): DownloadSnapshot? {
        return manager.query(query).use { cursor ->
            if (!cursor.moveToFirst()) return@use null
            val status = cursor.intColumn(DownloadManager.COLUMN_STATUS)
            val downloaded = cursor.longColumn(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
            val total = cursor.longColumn(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
            val progress = if (total > 0L) ((downloaded * 100L) / total).toInt().coerceIn(0, 100) else null
            val reason = cursor.intColumnOrNull(DownloadManager.COLUMN_REASON)
            DownloadSnapshot(
                status = status,
                progress = progress,
                downloadedBytes = downloaded,
                totalBytes = total.takeIf { it > 0L },
                reason = reason,
            )
        }
    }

    private fun verifyAndBuildSuccessResult(
        context: Context,
        manager: DownloadManager,
        downloadId: Long,
        expectedSha256: String?,
    ): OtaDownloadResult {
        val uri =
            manager.getUriForDownloadedFile(downloadId)
                ?: return OtaDownloadResult.Error("Downloaded APK uri unavailable.")
        if (expectedSha256.isNullOrBlank()) {
            return OtaDownloadResult.Success(uri)
        }
        val actualHash =
            computeSha256(context, uri)
                ?: return OtaDownloadResult.Error("Failed to verify update file.")
        if (!actualHash.equals(expectedSha256, ignoreCase = true)) {
            manager.remove(downloadId)
            return OtaDownloadResult.Error("APK integrity check failed.")
        }
        return OtaDownloadResult.Success(uri)
    }

    private fun computeSha256(
        context: Context,
        uri: Uri,
    ): String? {
        return runCatching {
            val digest = MessageDigest.getInstance("SHA-256")
            context.contentResolver.openInputStream(uri)?.use { input ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                while (true) {
                    val read = input.read(buffer)
                    if (read <= 0) break
                    digest.update(buffer, 0, read)
                }
            } ?: return null
            digest.digest().joinToString("") { byte -> "%02x".format(byte) }
        }.getOrNull()
    }
}

private data class DownloadSnapshot(
    val status: Int,
    val progress: Int?,
    val downloadedBytes: Long,
    val totalBytes: Long?,
    val reason: Int?,
)

private fun DownloadSnapshot.toProgressSample(
    previousSampleTimeMs: Long,
    previousDownloadedBytes: Long,
): OtaDownloadProgress {
    val now = System.currentTimeMillis()
    val elapsedMs = now - previousSampleTimeMs
    val safeDownloadedBytes = downloadedBytes.coerceAtLeast(0L)
    val speedBytesPerSecond =
        if (elapsedMs > 0L) {
            val byteDelta = (safeDownloadedBytes - previousDownloadedBytes).coerceAtLeast(0L)
            ((byteDelta * 1000L) / elapsedMs).coerceAtLeast(0L)
        } else {
            null
        }
    return OtaDownloadProgress(
        progressPercent = progress,
        downloadedBytes = safeDownloadedBytes,
        totalBytes = totalBytes,
        bytesPerSecond = speedBytesPerSecond,
    )
}

private fun Cursor.intColumn(name: String): Int = getInt(getColumnIndexOrThrow(name))

private fun Cursor.intColumnOrNull(name: String): Int? {
    val index = getColumnIndex(name)
    return if (index >= 0) getInt(index) else null
}

private fun Cursor.longColumn(name: String): Long = getLong(getColumnIndexOrThrow(name))
