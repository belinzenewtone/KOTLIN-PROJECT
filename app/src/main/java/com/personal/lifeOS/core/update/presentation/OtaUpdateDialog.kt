package com.personal.lifeOS.core.update.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.personal.lifeOS.core.update.OtaUpdateManifest
import java.util.Locale
import kotlin.math.log10
import kotlin.math.pow

@Composable
internal fun OtaUpdateDialog(
    appName: String,
    manifest: OtaUpdateManifest,
    state: OtaPromptUiState,
    hasDownloadedApk: Boolean,
    callbacks: OtaDialogCallbacks,
) {
    Dialog(
        onDismissRequest = {
            if (!manifest.mandatory && !state.isDownloading) {
                callbacks.onDismiss()
            }
        },
        properties =
            DialogProperties(
                dismissOnBackPress = !manifest.mandatory && !state.isDownloading,
                dismissOnClickOutside = false,
                usePlatformDefaultWidth = false,
            ),
    ) {
        Card(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
            shape = RoundedCornerShape(24.dp),
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OtaDialogHeader(
                    appName = appName,
                    manifest = manifest,
                    canClose = !manifest.mandatory && !state.isDownloading,
                    onClose = callbacks.onDismiss,
                )
                OtaDialogBody(manifest = manifest)
                OtaDialogProgress(state = state)
                OtaDialogStatus(state = state)
                OtaDialogActions(
                    state = state,
                    manifest = manifest,
                    hasDownloadedApk = hasDownloadedApk,
                    callbacks = callbacks,
                )
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = callbacks.onWebsite,
                ) {
                    Text("Website")
                }
            }
        }
    }
}

@Composable
private fun OtaDialogHeader(
    appName: String,
    manifest: OtaUpdateManifest,
    canClose: Boolean,
    onClose: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier =
                Modifier
                    .background(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        shape = CircleShape,
                    )
                    .padding(horizontal = 10.dp, vertical = 6.dp),
        ) {
            Text(
                text = "S",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        Text(
            text = manifest.title ?: "Update $appName",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.weight(1f),
        )

        if (canClose) {
            IconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "Close update dialog",
                )
            }
        }
    }
}

@Composable
private fun OtaDialogBody(manifest: OtaUpdateManifest) {
    Text(
        text =
            manifest.message
                ?: "A newer update is available. Please update now for the latest fixes and features.",
        style = MaterialTheme.typography.bodyMedium,
    )
    manifest.changelog?.takeIf { it.isNotBlank() }?.let {
        Text(
            text = it.trim(),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun OtaDialogProgress(state: OtaPromptUiState) {
    if (!state.isDownloading && state.downloadPercent == null) return

    LinearProgressIndicator(
        progress = { (state.downloadPercent ?: 0).coerceIn(0, 100) / 100f },
        modifier = Modifier.fillMaxWidth(),
    )

    val percentage = "${state.downloadPercent ?: 0}%"
    val transferred =
        if (state.totalBytes != null) {
            "${formatBytes(state.downloadedBytes)}/${formatBytes(state.totalBytes ?: 0L)}"
        } else {
            formatBytes(state.downloadedBytes)
        }
    val speed =
        state.downloadSpeedBytesPerSec
            ?.takeIf { it > 0L }
            ?.let { " • ${formatBytes(it)}/s" }
            .orEmpty()

    Text(
        text = "$percentage  $transferred$speed",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun OtaDialogStatus(state: OtaPromptUiState) {
    state.statusMessage?.takeIf { it.isNotBlank() }?.let {
        Text(
            text = it,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun OtaDialogActions(
    state: OtaPromptUiState,
    manifest: OtaUpdateManifest,
    hasDownloadedApk: Boolean,
    callbacks: OtaDialogCallbacks,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        when {
            state.isDownloading -> {
                OutlinedButton(
                    modifier = Modifier.weight(1f),
                    onClick = callbacks.onCancelDownload,
                ) {
                    Text("Cancel")
                }
            }

            !manifest.mandatory -> {
                OutlinedButton(
                    modifier = Modifier.weight(1f),
                    onClick = callbacks.onLater,
                ) {
                    Text("Later")
                }
            }
        }

        Button(
            modifier = Modifier.weight(1f),
            onClick = callbacks.onPrimaryAction,
        ) {
            Text(primaryButtonLabel(state = state, hasDownloadedApk = hasDownloadedApk))
        }
    }
}

private fun primaryButtonLabel(
    state: OtaPromptUiState,
    hasDownloadedApk: Boolean,
): String {
    return when {
        state.isDownloading -> "Updating..."
        hasDownloadedApk -> "Install now"
        else -> "Update now"
    }
}

private fun formatBytes(value: Long): String {
    if (value <= 0L) return "0 B"
    val units = listOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (log10(value.toDouble()) / log10(1024.0)).toInt().coerceIn(0, units.lastIndex)
    val scaled = value / 1024.0.pow(digitGroups.toDouble())
    return String.format(Locale.US, "%.1f %s", scaled, units[digitGroups])
}
