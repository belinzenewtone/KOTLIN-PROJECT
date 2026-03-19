package com.personal.lifeOS.core.ui.designsystem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.personal.lifeOS.core.ui.model.AssistantActionProposalUiModel

@Composable
fun AssistantActionCard(
    proposal: AssistantActionProposalUiModel,
    modifier: Modifier = Modifier,
    onApprove: () -> Unit,
    onReject: () -> Unit,
) {
    AppCard(
        modifier = modifier,
        elevated = true,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(AppDesignTokens.spacing.sm),
        ) {
            Text(
                text = proposal.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = proposal.summary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            SyncStatusPill(status = proposal.riskLabel)
            Row(
                horizontalArrangement = Arrangement.spacedBy(AppDesignTokens.spacing.sm),
            ) {
                OutlinedButton(onClick = onReject) {
                    Text("Cancel")
                }
                Button(onClick = onApprove) {
                    Text("Apply")
                }
            }
        }
    }
}
