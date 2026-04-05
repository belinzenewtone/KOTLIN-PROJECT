package com.personal.lifeOS.core.ui.designsystem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.personal.lifeOS.core.ui.model.ImportHealthUiModel

@Composable
fun ImportHealthPanel(
    model: ImportHealthUiModel,
    modifier: Modifier = Modifier,
    onReview: () -> Unit = {},
) {
    AppCard(
        modifier = modifier.fillMaxWidth(),
        elevated = true,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(AppDesignTokens.spacing.sm),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Import Health",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                TextButton(onClick = onReview) {
                    Text("Review")
                }
            }
            Text(
                text =
                    "${model.importedCount} imported | " +
                        "${model.pendingReviewCount} pending | " +
                        "${model.duplicateCount} duplicates | " +
                        "${model.parseFailureCount} parse issues",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            model.lastImportSummary?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
