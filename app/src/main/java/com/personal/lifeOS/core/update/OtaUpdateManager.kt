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
import androidx.core.net.toUri
import com.personal.lifeOS.core.observability.AppTelemetry
import com.google.gson.FieldNamingPolicy
import com.google.gson.GsonBuilder
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

data class OtaUpdateManifest(
    @SerializedName("version_code")
    val versionCode: Long,
    @SerializedName("version_name")
    val versionName: String? = null,
    @SerializedName(value = "apk_url", alternate = ["download_url"])
    val apkUrl: String,
    @SerializedName(value = "apk_sha256", alternate = ["checksum_sha256"])
    val apkSha256: String? = null,
    @SerializedName(value = "changelog", alternate = ["release_notes"])
    val changelog: String? = null,
    @SerializedName(value = "mandatory", alternate = ["required"])
    val mandatory: Boolean = false,
    @SerializedName("title")
    val title: String? = null,
    @SerializedName("message")
    val message: String? = null,
    @SerializedName("website_url")
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

private val otaManifestGson =
    GsonBuilder()
        .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
        .create()

/**
 * APK OTA update manager for non-Play distribution.
 */
@Suppress("TooManyFunctions")
object OtaUpdateManager {
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

            val result =
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

                val manifest =
                    parseManifestBody(body)
                        ?: return@runCatching OtaCheckResult.Error("Invalid OTA manifest JSON.")

                val currentVersionCode = getCurrentVersionCode(context)
                evaluateManifest(manifest, currentVersionCode)
            }.getOrElse { OtaCheckResult.Error(it.message ?: "Failed to check for updates.") }

            trackOtaCheckResult(result)
            result
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
                DownloadManager.Request(manifest.apkUrl.toUri())
                    .setMimeType("application/vnd.android.package-archive")
                    .setTitle("Updating to v${manifest.versionName ?: manifest.versionCode}")
                    .setDescription("Tap to return to the app")
                    // VISIBILITY_VISIBLE shows progress during download (not just on completion),
                    // so the user can see status in the notification shade if they background the app.
                    .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
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

            val result =
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
            AppTelemetry.trackEvent(
                name = "ota_download_result",
                attributes =
                    mapOf(
                        "status" to
                            when (result) {
                                is OtaDownloadResult.Success -> "success"
                                OtaDownloadResult.Cancelled -> "cancelled"
                                is OtaDownloadResult.Error -> "error"
                            },
                        "target_version" to manifest.versionCode.toString(),
                    ),
            )
            result
        }

    fun launchInstaller(
        activity: Activity,
        apkUri: Uri,
    ): OtaInstallResult {
        AppTelemetry.trackEvent(
            name = "ota_install_start",
            attributes = mapOf("uri_scheme" to apkUri.scheme.orEmpty()),
            captureAsMessage = true,
        )
        if (!activity.packageManager.canRequestPackageInstalls()) {
            val settingsIntent =
                Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    data = "package:${activity.packageName}".toUri()
                }
            activity.startActivity(settingsIntent)
            AppTelemetry.trackEvent(
                name = "ota_install_permission_required",
                attributes = mapOf("package" to activity.packageName),
            )
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
            AppTelemetry.trackEvent(
                name = "ota_install_complete",
                attributes = mapOf("status" to "installer_opened"),
                captureAsMessage = true,
            )
            OtaInstallResult.Started
        } catch (e: ActivityNotFoundException) {
            AppTelemetry.captureError(
                throwable = e,
                context = mapOf("event" to "ota_install", "error_type" to "activity_not_found"),
            )
            OtaInstallResult.Error(e.message ?: "No installer available on device.")
        } catch (e: SecurityException) {
            AppTelemetry.captureError(
                throwable = e,
                context = mapOf("event" to "ota_install", "error_type" to "security"),
            )
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
            Intent(Intent.ACTION_VIEW, target.toUri()).apply {
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

    private fun trackOtaCheckResult(result: OtaCheckResult) {
        val (status, versionCode) =
            when (result) {
                is OtaCheckResult.UpdateAvailable -> "update_available" to result.manifest.versionCode.toString()
                OtaCheckResult.UpToDate -> "up_to_date" to ""
                OtaCheckResult.NotConfigured -> "not_configured" to ""
                is OtaCheckResult.Error -> "error" to ""
            }
        val attrs = mutableMapOf("status" to status)
        if (versionCode.isNotBlank()) {
            attrs["target_version"] = versionCode
        }
        AppTelemetry.trackEvent(
            name = "ota_check",
            attributes = attrs,
            captureAsMessage = status == "error",
        )
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

internal fun parseManifestBody(body: String): OtaUpdateManifest? {
    return runCatching {
        otaManifestGson.fromJson(body, OtaUpdateManifest::class.java)
    }.getOrNull()
}

internal fun evaluateManifest(
    manifest: OtaUpdateManifest,
    currentVersionCode: Long,
): OtaCheckResult {
    if (manifest.apkUrl.isBlank() || manifest.versionCode <= 0L) {
        return OtaCheckResult.Error("Invalid OTA manifest fields.")
    }
    return if (manifest.versionCode > currentVersionCode) {
        OtaCheckResult.UpdateAvailable(manifest)
    } else {
        OtaCheckResult.UpToDate
    }
}
