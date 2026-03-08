package com.personal.lifeOS.features.export.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.personal.lifeOS.ui.components.GlassCard
import com.personal.lifeOS.ui.theme.AppSpacing
import com.personal.lifeOS.ui.theme.BackgroundDark
import com.personal.lifeOS.ui.theme.Error
import com.personal.lifeOS.ui.theme.TextSecondary

@Composable
fun ExportScreen(viewModel: ExportViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(BackgroundDark)
                .padding(horizontal = AppSpacing.ScreenHorizontal)
                .padding(top = AppSpacing.ScreenTop, bottom = AppSpacing.Section),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.Section),
    ) {
        Text("Export", style = MaterialTheme.typography.titleLarge)
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Export all local data to a JSON file (app documents directory).",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                )
                Button(
                    onClick = viewModel::exportJson,
                    enabled = !state.isExporting,
                ) {
                    if (state.isExporting) {
                        CircularProgressIndicator(strokeWidth = 2.dp)
                    } else {
                        Text("Export JSON")
                    }
                }
            }
        }

        state.result?.let { result ->
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Export complete", style = MaterialTheme.typography.titleMedium)
                    Text("Items: ${result.itemCount}", style = MaterialTheme.typography.bodyMedium)
                    Text(result.filePath, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
            }
        }

        state.error?.let { message ->
            Text(message, style = MaterialTheme.typography.bodySmall, color = Error)
        }
    }
}
