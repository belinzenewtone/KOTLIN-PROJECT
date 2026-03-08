package com.personal.lifeOS.core.update.presentation

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.personal.lifeOS.BuildConfig
import com.personal.lifeOS.R
import com.personal.lifeOS.core.update.OtaCheckResult
import com.personal.lifeOS.core.update.OtaDownloadResult
import com.personal.lifeOS.core.update.OtaInstallResult
import com.personal.lifeOS.core.update.OtaUpdateManager
import com.personal.lifeOS.core.update.OtaUpdateManifest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@Composable
fun OtaUpdatePromptHost(shouldCheckForUpdates: Boolean) {
    if (!shouldCheckForUpdates) return

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var uiState by rememberSaveable(stateSaver = OtaPromptUiState.Saver) {
        mutableStateOf(OtaPromptUiState())
    }
    var manifest by remember { mutableStateOf<OtaUpdateManifest?>(null) }
    var downloadedApkUri by remember { mutableStateOf<Uri?>(null) }
    var downloadJob by remember { mutableStateOf<Job?>(null) }

    LaunchedEffect(shouldCheckForUpdates, uiState.hasCheckedThisSession, uiState.isChecking) {
        uiState =
            checkForUpdateIfNeeded(
                context = context,
                shouldCheckForUpdates = shouldCheckForUpdates,
                state = uiState,
                onManifestAvailable = { manifest = it },
            )
    }

    val activeManifest = manifest ?: return
    if (!uiState.showDialog) return

    OtaUpdateDialog(
        appName = context.getString(R.string.app_name),
        manifest = activeManifest,
        state = uiState,
        hasDownloadedApk = downloadedApkUri != null,
        callbacks =
            OtaDialogCallbacks(
                onDismiss = { uiState = uiState.dismissForVersion(activeManifest.versionCode) },
                onCancelDownload = {
                    uiState =
                        cancelActiveDownload(
                            context = context,
                            state = uiState,
                            activeDownloadJob = downloadJob,
                        )
                },
                onLater = { uiState = uiState.dismissForVersion(activeManifest.versionCode) },
                onWebsite = {
                    uiState =
                        openUpdateWebsite(
                            context = context,
                            state = uiState,
                            manifest = activeManifest,
                        )
                },
                onPrimaryAction = {
                    val updatedState =
                        handlePrimaryAction(
                            context = context,
                            state = uiState,
                            manifest = activeManifest,
                            downloadedApkUri = downloadedApkUri,
                            runtime =
                                DownloadActionRuntime(
                                    scope = scope,
                                    onDownloadStarted = { downloadJob = it },
                                    onDownloaded = { downloadedApkUri = it },
                                    onStateUpdated = { uiState = it },
                                ),
                        )
                    uiState = updatedState
                },
            ),
    )
}

private suspend fun checkForUpdateIfNeeded(
    context: Context,
    shouldCheckForUpdates: Boolean,
    state: OtaPromptUiState,
    onManifestAvailable: (OtaUpdateManifest) -> Unit,
): OtaPromptUiState {
    if (!shouldCheckForUpdates || state.hasCheckedThisSession || state.isChecking) return state
    if (BuildConfig.OTA_MANIFEST_URL.isBlank()) {
        return state.copy(hasCheckedThisSession = true)
    }

    var nextState = state.copy(isChecking = true)
    when (val result = OtaUpdateManager.checkForUpdate(context, BuildConfig.OTA_MANIFEST_URL)) {
        is OtaCheckResult.UpdateAvailable -> {
            onManifestAvailable(result.manifest)
            val shouldShow =
                result.manifest.mandatory || result.manifest.versionCode != state.skippedVersionCode
            nextState = nextState.copy(showDialog = shouldShow)
        }

        OtaCheckResult.UpToDate -> Unit
        OtaCheckResult.NotConfigured -> Unit
        is OtaCheckResult.Error -> nextState = nextState.copy(statusMessage = result.message)
    }

    return nextState.copy(
        isChecking = false,
        hasCheckedThisSession = true,
    )
}

private fun cancelActiveDownload(
    context: Context,
    state: OtaPromptUiState,
    activeDownloadJob: Job?,
): OtaPromptUiState {
    activeDownloadJob?.cancel()
    if (state.activeDownloadId > 0L) {
        OtaUpdateManager.cancelDownload(context, state.activeDownloadId)
    }
    return state.copy(
        isDownloading = false,
        activeDownloadId = -1L,
        statusMessage = "Download cancelled.",
    )
}

private fun openUpdateWebsite(
    context: Context,
    state: OtaPromptUiState,
    manifest: OtaUpdateManifest,
): OtaPromptUiState {
    val target = manifest.websiteUrl ?: manifest.apkUrl
    val opened = OtaUpdateManager.openWebsite(context, target)
    return if (opened) state else state.copy(statusMessage = "Could not open website link.")
}

private fun handlePrimaryAction(
    context: Context,
    state: OtaPromptUiState,
    manifest: OtaUpdateManifest,
    downloadedApkUri: Uri?,
    runtime: DownloadActionRuntime,
): OtaPromptUiState {
    if (downloadedApkUri != null) {
        return installDownloadedApk(context, state, downloadedApkUri)
    }
    if (state.isDownloading) return state

    val activity = context.findActivity()
    if (activity == null) {
        return state.copy(statusMessage = "Update flow is unavailable in this context.")
    }

    val nextState =
        state.copy(
            isDownloading = true,
            statusMessage = "Downloading update...",
            downloadPercent = 0,
        )
    runtime.onDownloadStarted(
        runtime.scope.launch {
            downloadAndInstallUpdate(
                context = context,
                activity = activity,
                manifest = manifest,
                startState = nextState,
                runtime = runtime,
            )
        },
    )
    return nextState
}

private fun installDownloadedApk(
    context: Context,
    state: OtaPromptUiState,
    downloadedApkUri: Uri,
): OtaPromptUiState {
    val activity = context.findActivity()
    if (activity == null) {
        return state.copy(statusMessage = "Install flow is unavailable in this context.")
    }
    return state.copy(
        statusMessage = mapInstallStatus(OtaUpdateManager.launchInstaller(activity, downloadedApkUri)),
    )
}

private suspend fun downloadAndInstallUpdate(
    context: Context,
    activity: Activity,
    manifest: OtaUpdateManifest,
    startState: OtaPromptUiState,
    runtime: DownloadActionRuntime,
) {
    when (
        val result =
            OtaUpdateManager.downloadUpdate(
                context = context,
                manifest = manifest,
                onEnqueued = { runtime.onStateUpdated(startState.copy(activeDownloadId = it)) },
                onProgress = { progress ->
                    runtime.onStateUpdated(startState.copy(downloadPercent = progress))
                },
                onProgressDetails = { details ->
                    runtime.onStateUpdated(
                        startState.copy(
                            downloadPercent = details.progressPercent,
                            downloadedBytes = details.downloadedBytes,
                            totalBytes = details.totalBytes,
                            downloadSpeedBytesPerSec = details.bytesPerSecond,
                        ),
                    )
                },
            )
    ) {
        is OtaDownloadResult.Success -> {
            runtime.onDownloaded(result.apkUri)
            val installMessage = mapInstallStatus(OtaUpdateManager.launchInstaller(activity, result.apkUri))
            runtime.onStateUpdated(
                startState.copy(
                    isDownloading = false,
                    activeDownloadId = -1L,
                    downloadPercent = 100,
                    statusMessage = installMessage,
                    showDialog = manifest.mandatory,
                ),
            )
        }

        OtaDownloadResult.Cancelled -> {
            runtime.onStateUpdated(
                startState.copy(
                    isDownloading = false,
                    activeDownloadId = -1L,
                    statusMessage = "Download cancelled.",
                ),
            )
        }

        is OtaDownloadResult.Error -> {
            runtime.onStateUpdated(
                startState.copy(
                    isDownloading = false,
                    activeDownloadId = -1L,
                    statusMessage = "Download failed: ${result.message}",
                ),
            )
        }
    }
}

private fun mapInstallStatus(result: OtaInstallResult): String {
    return when (result) {
        OtaInstallResult.Started -> "Installer opened. Confirm installation to complete update."
        OtaInstallResult.RequiresUnknownSourcesPermission ->
            "Allow installs from this app, then tap install again."

        is OtaInstallResult.Error -> "Install launch failed: ${result.message}"
    }
}

private tailrec fun Context.findActivity(): Activity? {
    return when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
}

private data class DownloadActionRuntime(
    val scope: CoroutineScope,
    val onDownloadStarted: (Job) -> Unit,
    val onDownloaded: (Uri) -> Unit,
    val onStateUpdated: (OtaPromptUiState) -> Unit,
)

internal data class OtaDialogCallbacks(
    val onDismiss: () -> Unit,
    val onCancelDownload: () -> Unit,
    val onLater: () -> Unit,
    val onWebsite: () -> Unit,
    val onPrimaryAction: () -> Unit,
)
