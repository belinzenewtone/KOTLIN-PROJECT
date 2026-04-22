package com.personal.lifeOS.core.update.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.Image
import com.personal.lifeOS.R
import com.personal.lifeOS.core.ui.designsystem.AppDesignTokens
import com.personal.lifeOS.core.update.OtaUpdateManifest
import java.util.Locale
import kotlin.math.log10
import kotlin.math.pow

@Composable
internal fun OtaUpdateDialog(
    appName: String,
    currentVersionName: String,
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
            shape = RoundedCornerShape(AppDesignTokens.radius.lg),
            colors =
                CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                ),
            border =
                BorderStroke(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.72f),
                ),
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OtaDialogHeader(
                    appName = appName,
                    currentVersionName = currentVersionName,
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
                    border = BorderStroke(1.4.dp, MaterialTheme.colorScheme.outline),
                    onClick = callbacks.onWebsite,
                    shape = RoundedCornerShape(AppDesignTokens.radius.pill),
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
    currentVersionName: String,
    manifest: OtaUpdateManifest,
    canClose: Boolean,
    onClose: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Image(
            painter = painterResource(id = R.mipmap.ic_launcher),
            contentDescription = "$appName icon",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape),
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = manifest.title ?: "Update $appName",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            // Version arrow: "v1.2.3 → v1.3.0"
            val newVersion = manifest.versionName ?: manifest.versionCode.toString()
            Text(
                text = "v$currentVersionName → v$newVersion",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

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
        color = MaterialTheme.colorScheme.onSurface,
    )

    // Collapsible changelog — collapsed by default so it doesn't overwhelm the dialog
    manifest.changelog?.takeIf { it.isNotBlank() }?.let { log ->
        var expanded by remember { mutableStateOf(false) }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (expanded) "What's new ▴" else "What's new ▾",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            AnimatedVisibility(visible = expanded) {
                Text(
                    text = log.trim(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun OtaDialogProgress(state: OtaPromptUiState) {
    if (!state.isDownloading && state.downloadPercent == null) return

    val targetProgress = (state.downloadPercent ?: 0).coerceIn(0, 100) / 100f
    val animatedProgress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = tween(durationMillis = 400),
        label = "ota_progress",
    )

    LinearProgressIndicator(
        progress = { animatedProgress },
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
                    border = BorderStroke(1.4.dp, MaterialTheme.colorScheme.outline),
                    onClick = callbacks.onCancelDownload,
                    shape = RoundedCornerShape(AppDesignTokens.radius.pill),
                ) {
                    Text("Cancel")
                }
            }

            !manifest.mandatory -> {
                OutlinedButton(
                    modifier = Modifier.weight(1f),
                    border = BorderStroke(1.4.dp, MaterialTheme.colorScheme.outline),
                    onClick = callbacks.onLater,
                    shape = RoundedCornerShape(AppDesignTokens.radius.pill),
                ) {
                    Text("Later")
                }
            }
        }

        Button(
            modifier = Modifier.weight(1f),
            onClick = callbacks.onPrimaryAction,
            shape = RoundedCornerShape(AppDesignTokens.radius.pill),
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
        state.downloadFailed -> "Retry"
        else -> "Update now"
    }
}

/**
 * Lightweight dialog shown while the OTA manifest is being fetched.
 * Dismissed automatically once the check completes (dialog is only composed when isChecking = true).
 */
@Composable
internal fun OtaCheckingDialog(appName: String) {
    Dialog(
        onDismissRequest = { /* not dismissible while checking */ },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
        ),
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            shape = RoundedCornerShape(AppDesignTokens.radius.lg),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            ),
            border = BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.72f),
            ),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(28.dp),
                    strokeWidth = 3.dp,
                )
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "Checking for updates",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "Looking for a newer version of $appName…",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

private fun formatBytes(value: Long): String {
    if (value <= 0L) return "0 B"
    val units = listOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (log10(value.toDouble()) / log10(1024.0)).toInt().coerceIn(0, units.lastIndex)
    val scaled = value / 1024.0.pow(digitGroups.toDouble())
    return String.format(Locale.US, "%.1f %s", scaled, units[digitGroups])
}
