package com.personal.lifeOS.features.settings.presentation

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.personal.lifeOS.BuildConfig
import com.personal.lifeOS.core.datastore.FeatureFlag
import com.personal.lifeOS.core.update.OtaCheckResult
import com.personal.lifeOS.core.update.OtaDownloadResult
import com.personal.lifeOS.core.update.OtaInstallResult
import com.personal.lifeOS.core.update.OtaUpdateManager
import com.personal.lifeOS.core.update.OtaUpdateManifest
import com.personal.lifeOS.ui.components.GlassCard
import com.personal.lifeOS.ui.theme.AppSpacing
import com.personal.lifeOS.ui.theme.AppThemeMode
import com.personal.lifeOS.ui.theme.BackgroundDark
import com.personal.lifeOS.ui.theme.TextSecondary
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(state.infoMessage) {
        if (state.infoMessage != null) {
            // Keep message short-lived in state.
            viewModel.clearInfoMessage()
        }
    }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(BackgroundDark)
                .padding(horizontal = AppSpacing.ScreenHorizontal)
                .padding(top = AppSpacing.ScreenTop, bottom = AppSpacing.Section),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.Section),
    ) {
        Text("Settings", style = MaterialTheme.typography.titleLarge)

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Theme Mode", style = MaterialTheme.typography.titleMedium)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    AppThemeMode.entries.forEach { mode ->
                        OutlinedButton(onClick = { viewModel.setThemeMode(mode) }) {
                            val marker = if (state.themeMode == mode) "• " else ""
                            Text("$marker${mode.name.lowercase().replaceFirstChar { it.uppercase() }}")
                        }
                    }
                }
            }
        }

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Notifications", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Disable to stop new reminders and clear scheduled alarms",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                    )
                }
                Switch(
                    checked = state.notificationsEnabled,
                    onCheckedChange = viewModel::setNotificationsEnabled,
                )
            }
        }

        OtaUpdateCard()
        DiagnosticsHealthCard(
            state = state,
            onRefreshFlags = viewModel::refreshFeatureFlags,
        )
    }
}

@Composable
private fun DiagnosticsHealthCard(
    state: SettingsUiState,
    onRefreshFlags: () -> Unit,
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Diagnostics", style = MaterialTheme.typography.titleMedium)

            Text(
                text =
                    "Sync queue: queued ${state.syncHealth.queued}, syncing ${state.syncHealth.syncing}, " +
                        "failed ${state.syncHealth.failed}, conflict ${state.syncHealth.conflict}",
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                text = "Last sync update: ${formatTimestamp(state.syncHealth.latestJobUpdatedAt)}",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
            )

            HorizontalDivider(color = TextSecondary.copy(alpha = 0.2f))

            Text(
                text =
                    "SMS import: imported ${state.importHealth.imported}, duplicate ${state.importHealth.duplicate}, " +
                        "failed ${state.importHealth.parseFailed}, pending ${state.importHealth.pending}",
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                text =
                    "Ignored ${state.importHealth.ignored}, recovered ${state.importHealth.recovered}, " +
                        "last import ${formatTimestamp(state.importHealth.latestImportAt)}",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
            )

            HorizontalDivider(color = TextSecondary.copy(alpha = 0.2f))

            val update = state.latestUpdateInfo
            Text(
                text =
                    if (update == null) {
                        "Update diagnostics: no successful checks recorded yet."
                    } else {
                        "Latest update check: v${update.versionName ?: update.versionCode.toString()} " +
                            "required=${update.required} at ${formatTimestamp(update.checkedAt)}"
                    },
                style = MaterialTheme.typography.bodySmall,
            )

            HorizontalDivider(color = TextSecondary.copy(alpha = 0.2f))

            Text("Feature flags", style = MaterialTheme.typography.titleSmall)
            FeatureFlag.entries.forEach { flag ->
                val enabled = state.featureFlags[flag] ?: flag.defaultEnabled
                Text(
                    text = "${flag.key}: ${if (enabled) "enabled" else "disabled"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                )
            }

            OutlinedButton(onClick = onRefreshFlags) {
                Text("Refresh flag snapshot")
            }
        }
    }
}

@Composable
private fun OtaUpdateCard() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var state by rememberSaveable(stateSaver = OtaCardUiState.Saver) { mutableStateOf(OtaCardUiState()) }
    var availableManifest by remember { mutableStateOf<OtaUpdateManifest?>(null) }
    var downloadedApkUri by remember { mutableStateOf<Uri?>(null) }

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("App Updates", style = MaterialTheme.typography.titleMedium)
            Text(
                text = state.statusMessage,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
            )

            availableManifest?.let { manifest ->
                Text(
                    text = "Update available: ${manifest.versionName ?: "v${manifest.versionCode}"}",
                    style = MaterialTheme.typography.bodySmall,
                )
                manifest.changelog?.takeIf { it.isNotBlank() }?.let {
                    Text(
                        text = "Changelog: $it",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                    )
                }
            }

            state.downloadProgress?.let { progress ->
                Text("Download progress: $progress%", style = MaterialTheme.typography.bodySmall)
            }

            OtaActionButtons(
                state = state,
                hasAvailableManifest = availableManifest != null,
                hasDownloadedApk = downloadedApkUri != null,
                onCheck = {
                    checkForApkUpdate(
                        scope = scope,
                        context = context,
                        setState = { state = it },
                        setAvailableManifest = { availableManifest = it },
                        setDownloadedApkUri = { downloadedApkUri = it },
                        currentState = state,
                    )
                },
                onDownloadAndInstall = {
                    val manifest = availableManifest ?: return@OtaActionButtons
                    downloadAndInstallUpdate(
                        scope = scope,
                        context = context,
                        manifest = manifest,
                        setState = { state = it },
                        setDownloadedApkUri = { downloadedApkUri = it },
                        currentState = state,
                    )
                },
                onInstallDownloaded = {
                    val activity = context.findActivity()
                    val apkUri = downloadedApkUri
                    state =
                        if (activity == null || apkUri == null) {
                            state.copy(statusMessage = "No downloaded APK available to install.")
                        } else {
                            state.copy(
                                statusMessage =
                                    mapInstallStatus(
                                        OtaUpdateManager.launchInstaller(activity, apkUri),
                                    ),
                            )
                        }
                },
            )
        }
    }
}

@Composable
private fun OtaActionButtons(
    state: OtaCardUiState,
    hasAvailableManifest: Boolean,
    hasDownloadedApk: Boolean,
    onCheck: () -> Unit,
    onDownloadAndInstall: () -> Unit,
    onInstallDownloaded: () -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(
            enabled = !state.isBusy,
            onClick = onCheck,
        ) {
            if (state.isBusy && !hasAvailableManifest) {
                CircularProgressIndicator(
                    strokeWidth = 2.dp,
                    modifier = Modifier.padding(end = 8.dp),
                )
            }
            Text(if (state.isBusy && !hasAvailableManifest) "Checking..." else "Check for updates")
        }

        OutlinedButton(
            enabled = !state.isBusy && hasAvailableManifest,
            onClick = onDownloadAndInstall,
        ) {
            if (state.isBusy && hasAvailableManifest) {
                CircularProgressIndicator(
                    strokeWidth = 2.dp,
                    modifier = Modifier.padding(end = 8.dp),
                )
            }
            Text(if (state.isBusy && hasAvailableManifest) "Downloading..." else "Download & install")
        }
    }

    if (hasDownloadedApk) {
        OutlinedButton(
            enabled = !state.isBusy,
            onClick = onInstallDownloaded,
        ) {
            Text("Install downloaded update")
        }
    }
}

private fun checkForApkUpdate(
    scope: CoroutineScope,
    context: Context,
    setState: (OtaCardUiState) -> Unit,
    setAvailableManifest: (OtaUpdateManifest?) -> Unit,
    setDownloadedApkUri: (Uri?) -> Unit,
    currentState: OtaCardUiState,
) {
    scope.launch {
        setState(currentState.copy(isBusy = true, downloadProgress = null))
        setAvailableManifest(null)
        setDownloadedApkUri(null)
        val nextMessage =
            when (val result = OtaUpdateManager.checkForUpdate(context, BuildConfig.OTA_MANIFEST_URL)) {
                is OtaCheckResult.UpdateAvailable -> {
                    setAvailableManifest(result.manifest)
                    "Update found. Ready to download and install."
                }

                OtaCheckResult.UpToDate -> "You're on the latest version."
                OtaCheckResult.NotConfigured -> "OTA_MANIFEST_URL is not configured in local.properties."
                is OtaCheckResult.Error -> "Update check failed: ${result.message}"
            }
        setState(currentState.copy(isBusy = false, statusMessage = nextMessage, downloadProgress = null))
    }
}

private fun downloadAndInstallUpdate(
    scope: CoroutineScope,
    context: Context,
    manifest: OtaUpdateManifest,
    setState: (OtaCardUiState) -> Unit,
    setDownloadedApkUri: (Uri?) -> Unit,
    currentState: OtaCardUiState,
) {
    val activity = context.findActivity()
    if (activity == null) {
        setState(currentState.copy(statusMessage = "Install flow unavailable in this context."))
        return
    }

    scope.launch {
        setState(currentState.copy(isBusy = true, statusMessage = "Downloading update APK..."))
        val downloadResult =
            OtaUpdateManager.downloadUpdate(
                context = context,
                manifest = manifest,
                onProgress = { progress ->
                    setState(currentState.copy(isBusy = true, downloadProgress = progress))
                },
            )
        when (downloadResult) {
            is OtaDownloadResult.Success -> {
                setDownloadedApkUri(downloadResult.apkUri)
                setState(
                    currentState.copy(
                        isBusy = false,
                        downloadProgress = 100,
                        statusMessage =
                            mapInstallStatus(
                                OtaUpdateManager.launchInstaller(activity, downloadResult.apkUri),
                            ),
                    ),
                )
            }

            OtaDownloadResult.Cancelled -> {
                setState(
                    currentState.copy(
                        isBusy = false,
                        statusMessage = "Download cancelled.",
                    ),
                )
            }

            is OtaDownloadResult.Error -> {
                setState(
                    currentState.copy(
                        isBusy = false,
                        statusMessage = "Download failed: ${downloadResult.message}",
                    ),
                )
            }
        }
    }
}

private data class OtaCardUiState(
    val isBusy: Boolean = false,
    val downloadProgress: Int? = null,
    val statusMessage: String = "Check and install APK updates from your OTA server.",
) {
    companion object {
        val Saver =
            androidx.compose.runtime.saveable.listSaver<OtaCardUiState, Any?>(
                save = { listOf(it.isBusy, it.downloadProgress, it.statusMessage) },
                restore = {
                    OtaCardUiState(
                        isBusy = it[0] as Boolean,
                        downloadProgress = it[1] as Int?,
                        statusMessage = it[2] as String,
                    )
                },
            )
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

private fun formatTimestamp(epochMillis: Long?): String {
    if (epochMillis == null || epochMillis <= 0L) return "N/A"
    val formatter =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            .withZone(ZoneId.systemDefault())
    return formatter.format(Instant.ofEpochMilli(epochMillis))
}
