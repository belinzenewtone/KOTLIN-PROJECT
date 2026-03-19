package com.personal.lifeOS.features.export.presentation

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.personal.lifeOS.features.export.domain.model.ExportDomain
import com.personal.lifeOS.features.export.domain.model.ExportFormat
import com.personal.lifeOS.features.export.domain.model.ExportHistoryItem
import com.personal.lifeOS.features.export.domain.model.ExportPreview
import com.personal.lifeOS.features.export.domain.model.ExportResult
import com.personal.lifeOS.platform.files.ExportShareHelper
import com.personal.lifeOS.ui.components.GlassCard
import com.personal.lifeOS.ui.theme.AppSpacing
import com.personal.lifeOS.ui.theme.BackgroundDark
import com.personal.lifeOS.ui.theme.Error
import com.personal.lifeOS.ui.theme.TextSecondary
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun ExportScreen(viewModel: ExportViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(BackgroundDark)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = AppSpacing.ScreenHorizontal)
                .padding(top = AppSpacing.ScreenTop, bottom = AppSpacing.Section),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.Section),
    ) {
        Text("Export", style = MaterialTheme.typography.titleLarge)
        ExportConfigurationCard(state = state, viewModel = viewModel)
        ExportPreviewCard(state.preview, state.isPreviewLoading)
        ExportResultCard(state.result)
        ExportHistoryCard(state.history)

        state.result?.let { result ->
            OutlinedButton(
                onClick = {
                    val shareIntent =
                        ExportShareHelper.createShareIntent(
                            context = context,
                            filePath = result.filePath,
                            mimeType = result.mimeType,
                        )
                    if (shareIntent != null) {
                        context.startActivity(Intent.createChooser(shareIntent, "Share export file"))
                    }
                },
            ) {
                Text("Share latest export")
            }
        }

        state.error?.let { message ->
            Text(message, style = MaterialTheme.typography.bodySmall, color = Error)
        }
    }
}

@Composable
private fun ExportConfigurationCard(
    state: ExportUiState,
    viewModel: ExportViewModel,
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                "Create JSON or CSV exports for selected domains and date windows.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
            )

            Text("Format", style = MaterialTheme.typography.titleSmall)
            OptionRow(
                options = ExportFormat.entries.map { it.name },
                selected = state.selectedFormat.name,
                onSelect = { selected -> viewModel.setFormat(ExportFormat.valueOf(selected)) },
            )

            Text("Domain", style = MaterialTheme.typography.titleSmall)
            OptionRow(
                options = state.selectedFormat.allowedDomains().map { it.name },
                selected = state.selectedDomain.name,
                onSelect = { selected -> viewModel.setDomain(ExportDomain.valueOf(selected)) },
            )

            Text("Date window", style = MaterialTheme.typography.titleSmall)
            OptionRow(
                options = ExportDatePreset.entries.map { it.name },
                selected = state.selectedDatePreset.name,
                onSelect = { selected -> viewModel.setDatePreset(ExportDatePreset.valueOf(selected)) },
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Encrypt file", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "Protect export with passphrase (AES-GCM).",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                    )
                }
                Switch(
                    checked = state.encryptionEnabled,
                    onCheckedChange = viewModel::setEncryptionEnabled,
                )
            }

            if (state.encryptionEnabled) {
                OutlinedTextField(
                    value = state.encryptionPassphrase,
                    onValueChange = viewModel::setEncryptionPassphrase,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Passphrase") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                )
            }

            Button(
                onClick = viewModel::export,
                enabled = !state.isExporting,
            ) {
                if (state.isExporting) {
                    CircularProgressIndicator(
                        strokeWidth = 2.dp,
                        modifier = Modifier.widthIn(min = 20.dp),
                    )
                } else {
                    Text("Export now")
                }
            }
        }
    }
}

@Composable
private fun ExportPreviewCard(
    preview: ExportPreview?,
    isLoading: Boolean,
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Preview", style = MaterialTheme.typography.titleMedium)
            if (isLoading) {
                Text("Preparing preview...", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                return@Column
            }
            if (preview == null) {
                Text("Preview unavailable.", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                return@Column
            }
            Text("Total items: ${preview.totalItems}", style = MaterialTheme.typography.bodyMedium)
            preview.perDomainCount.forEach { (domain, count) ->
                Text(
                    "${domain.name.lowercase().replace('_', ' ')}: $count",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                )
            }
        }
    }
}

@Composable
private fun ExportResultCard(result: ExportResult?) {
    if (result == null) return

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Export complete", style = MaterialTheme.typography.titleMedium)
            Text("Items: ${result.itemCount}", style = MaterialTheme.typography.bodyMedium)
            Text("Path: ${result.filePath}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            Text(
                "Time: ${formatTimestamp(result.exportedAt)}",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
            )
        }
    }
}

@Composable
private fun ExportHistoryCard(history: List<ExportHistoryItem>) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("History", style = MaterialTheme.typography.titleMedium)
            if (history.isEmpty()) {
                Text("No exports yet.", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                return@Column
            }
            history.take(8).forEachIndexed { index, item ->
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        "${item.domain.name.lowercase()} · ${item.format.name} · ${item.status}",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Text(
                        "${formatTimestamp(item.exportedAt)} · items ${item.itemCount}",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                    )
                    item.errorMessage?.let { error ->
                        Text(error, style = MaterialTheme.typography.labelSmall, color = Error)
                    }
                }
                if (index < history.lastIndex) {
                    HorizontalDivider(color = TextSecondary.copy(alpha = 0.2f))
                }
            }
        }
    }
}

@Composable
private fun OptionRow(
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { option ->
            OutlinedButton(onClick = { onSelect(option) }) {
                val marker = if (selected == option) "• " else ""
                Text("$marker${option.lowercase().replace('_', ' ')}")
            }
        }
    }
}

private fun ExportFormat.allowedDomains(): List<ExportDomain> {
    return when (this) {
        ExportFormat.JSON -> ExportDomain.entries
        ExportFormat.CSV -> ExportDomain.entries.filterNot { it == ExportDomain.ALL }
    }
}

private fun formatTimestamp(epochMillis: Long): String {
    val formatter =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            .withZone(ZoneId.systemDefault())
    return formatter.format(Instant.ofEpochMilli(epochMillis))
}
