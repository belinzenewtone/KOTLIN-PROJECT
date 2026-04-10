package com.personal.lifeOS.features.settings.presentation

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.core.content.pm.PackageInfoCompat
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.foundation.BorderStroke
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.personal.lifeOS.core.datastore.FeatureFlag
import com.personal.lifeOS.core.update.OtaInstallResult
import com.personal.lifeOS.core.update.OtaUpdateManager
import com.personal.lifeOS.core.ui.designsystem.AppCard
import com.personal.lifeOS.core.ui.designsystem.AppDesignTokens
import com.personal.lifeOS.core.ui.designsystem.InlineBanner
import com.personal.lifeOS.core.ui.designsystem.InlineBannerTone
import com.personal.lifeOS.core.ui.designsystem.PageScaffold
import com.personal.lifeOS.core.ui.designsystem.TopBanner
import com.personal.lifeOS.core.ui.designsystem.TopBannerTone
import com.personal.lifeOS.ui.theme.AppThemeMode
import com.personal.lifeOS.ui.theme.AppSpacing
import kotlinx.coroutines.flow.collectLatest
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.SettingsBrightness
import androidx.compose.material3.Icon
import androidx.compose.ui.draw.clip
import com.personal.lifeOS.core.ui.designsystem.LifeOSSwitch
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.tween
import com.personal.lifeOS.BuildConfig

@Composable
@Suppress("LongMethod")
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onBack: (() -> Unit)? = null,
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val installedVersionLabel =
        runCatching {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            val versionName = packageInfo.versionName ?: BuildConfig.VERSION_NAME
            val versionCode = PackageInfoCompat.getLongVersionCode(packageInfo)
            "Installed version: $versionName (build $versionCode)"
        }.getOrElse {
            "Installed version: ${BuildConfig.VERSION_NAME} (build ${BuildConfig.VERSION_CODE})"
        }

    LaunchedEffect(viewModel, context) {
        viewModel.uiEffects.collectLatest { effect ->
            when (effect) {
                is SettingsUiEffect.LaunchOtaInstaller -> {
                    val result =
                        context.findActivity()?.let { activity ->
                            OtaUpdateManager.launchInstaller(activity, effect.apkUri)
                        } ?: OtaInstallResult.Error("Install flow unavailable in this context.")
                    viewModel.onEvent(SettingsUiEvent.InstallerResult(result))
                }
            }
        }
    }

    PageScaffold(
        headerEyebrow = "Preferences",
        title = "Settings",
        subtitle = "Preferences and customization",
        onBack = onBack,
        contentPadding = PaddingValues(bottom = AppSpacing.BottomSafeWithFloatingNav),
        topBanner = {
            state.infoMessage?.let {
                TopBanner(
                    message = it,
                    tone = TopBannerTone.SUCCESS,
                    onDismiss = {
                        viewModel.onEvent(SettingsUiEvent.ClearInfoMessage)
                    },
                )
            }
        },
    ) {
        LaunchedEffect(state.infoMessage) {
            if (state.infoMessage != null) {
                // Auto-dismiss after 3 seconds
                kotlinx.coroutines.delay(3000)
                viewModel.onEvent(SettingsUiEvent.ClearInfoMessage)
            }
        }

        AppCard(elevated = true) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Appearance", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Choose how the app looks on your device.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                ThemeModeToggle(
                    selected = state.themeMode,
                    onSelect = { mode -> viewModel.onEvent(SettingsUiEvent.SetThemeMode(mode)) },
                )
            }
        }

        AppCard(elevated = true) {
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
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                LifeOSSwitch(
                    checked = state.notificationsEnabled,
                    onCheckedChange = { enabled ->
                        viewModel.onEvent(SettingsUiEvent.ToggleNotifications(enabled))
                    },
                )
            }
        }

        OtaUpdateCard(
            installedVersionLabel = installedVersionLabel,
            state = state.otaUpdate,
            onCheck = { viewModel.onEvent(SettingsUiEvent.CheckForOtaUpdate) },
            onDownloadAndInstall = { viewModel.onEvent(SettingsUiEvent.DownloadAndInstallOtaUpdate) },
            onInstallDownloaded = { viewModel.onEvent(SettingsUiEvent.InstallDownloadedOtaUpdate) },
        )
        DiagnosticsHealthCard(
            state = state,
            onRefreshFlags = { viewModel.onEvent(SettingsUiEvent.RefreshFeatureFlags) },
        )
    }
}

@Composable
private fun DiagnosticsHealthCard(
    state: SettingsUiState,
    onRefreshFlags: () -> Unit,
) {
    AppCard(elevated = true) {
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
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))

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
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))

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

            HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))

            Text("Feature flags", style = MaterialTheme.typography.titleSmall)
            FeatureFlag.entries.forEach { flag ->
                val enabled = state.featureFlags[flag] ?: flag.defaultEnabled
                Text(
                    text = "${flag.key}: ${if (enabled) "enabled" else "disabled"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            OutlinedButton(onClick = onRefreshFlags) {
                Text("Refresh flag snapshot")
            }
        }
    }
}

@Composable
private fun OtaUpdateCard(
    installedVersionLabel: String,
    state: SettingsOtaUiState,
    onCheck: () -> Unit,
    onDownloadAndInstall: () -> Unit,
    onInstallDownloaded: () -> Unit,
) {
    AppCard(elevated = true) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("App Updates", style = MaterialTheme.typography.titleMedium)
            Text(
                text = installedVersionLabel,
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                text = state.statusMessage,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            state.availableUpdate?.let { update ->
                Text(
                    text = "Update available: ${update.versionName ?: "v${update.versionCode}"}",
                    style = MaterialTheme.typography.bodySmall,
                )
                update.changelog?.takeIf { it.isNotBlank() }?.let {
                    Text(
                        text = "Changelog: $it",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            state.downloadProgress?.let { progress ->
                Text("Download progress: $progress%", style = MaterialTheme.typography.bodySmall)
            }

            OtaActionButtons(
                state = state,
                onCheck = onCheck,
                onDownloadAndInstall = onDownloadAndInstall,
                onInstallDownloaded = onInstallDownloaded,
            )
        }
    }
}

@Composable
private fun OtaActionButtons(
    state: SettingsOtaUiState,
    onCheck: () -> Unit,
    onDownloadAndInstall: () -> Unit,
    onInstallDownloaded: () -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(
            enabled = !state.isBusy,
            border = BorderStroke(1.4.dp, MaterialTheme.colorScheme.outline),
            onClick = onCheck,
        ) {
            if (state.isBusy && state.availableUpdate == null) {
                CircularProgressIndicator(strokeWidth = 2.dp)
            } else {
                Text("Check for updates")
            }
        }

        OutlinedButton(
            enabled = !state.isBusy && state.availableUpdate != null,
            border = BorderStroke(1.4.dp, MaterialTheme.colorScheme.outline),
            onClick = onDownloadAndInstall,
        ) {
            if (state.isBusy && state.availableUpdate != null) {
                CircularProgressIndicator(strokeWidth = 2.dp)
            } else {
                Text("Download & install")
            }
        }
    }

    if (state.hasDownloadedApk) {
        OutlinedButton(
            enabled = !state.isBusy,
            border = BorderStroke(1.4.dp, MaterialTheme.colorScheme.outline),
            onClick = onInstallDownloaded,
        ) {
            Text("Install downloaded update")
        }
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

@Composable
private fun ThemeModeToggle(
    selected: com.personal.lifeOS.ui.theme.AppThemeMode,
    onSelect: (com.personal.lifeOS.ui.theme.AppThemeMode) -> Unit,
) {
    val modes = listOf(
        Triple(com.personal.lifeOS.ui.theme.AppThemeMode.LIGHT,  Icons.Outlined.LightMode,         "Light"),
        Triple(com.personal.lifeOS.ui.theme.AppThemeMode.SYSTEM, Icons.Outlined.SettingsBrightness, "Auto"),
        Triple(com.personal.lifeOS.ui.theme.AppThemeMode.DARK,   Icons.Outlined.DarkMode,           "Dark"),
    )
    val shape = RoundedCornerShape(AppDesignTokens.radius.sm)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
            .height(48.dp),
    ) {
        modes.forEach { (mode, icon, label) ->
            val isSelected = selected == mode
            // Animate background and content colours for a smooth feel
            val bgColor by animateColorAsState(
                targetValue = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                              else MaterialTheme.colorScheme.surface,
                animationSpec = tween(durationMillis = 240, easing = EaseInOut),
                label = "toggleBg_$label",
            )
            val contentColor by animateColorAsState(
                targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                              else MaterialTheme.colorScheme.onSurfaceVariant,
                animationSpec = tween(durationMillis = 240, easing = EaseInOut),
                label = "toggleContent_$label",
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(bgColor)
                    .clickable { onSelect(mode) },
                contentAlignment = androidx.compose.ui.Alignment.Center,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = contentColor,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium,
                        color = contentColor,
                    )
                }
            }
        }
    }
}
