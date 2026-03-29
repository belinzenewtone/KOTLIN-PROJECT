package com.personal.lifeOS.core.ui.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.personal.lifeOS.core.ui.model.SyncStatusUiModel

@Composable
fun SyncStatusPill(
    status: SyncStatusUiModel,
    modifier: Modifier = Modifier,
) {
    val tone =
        when (status) {
            SyncStatusUiModel.SYNCED -> AppSemanticTone.SUCCESS
            SyncStatusUiModel.SYNCING -> AppSemanticTone.INFO
            SyncStatusUiModel.FAILED -> AppSemanticTone.ERROR
            SyncStatusUiModel.CONFLICT -> AppSemanticTone.WARNING
            SyncStatusUiModel.LOCAL_ONLY -> AppSemanticTone.WARNING
            SyncStatusUiModel.QUEUED -> AppSemanticTone.INFO
            SyncStatusUiModel.TOMBSTONED -> AppSemanticTone.WARNING
            SyncStatusUiModel.UNKNOWN -> AppSemanticTone.INFO
        }
    val semantic = AppDesignTokens.semanticColors(tone)

    val label =
        when (status) {
            SyncStatusUiModel.SYNCED -> "Synced"
            SyncStatusUiModel.SYNCING -> "Syncing"
            SyncStatusUiModel.FAILED -> "Failed"
            SyncStatusUiModel.CONFLICT -> "Conflict"
            SyncStatusUiModel.LOCAL_ONLY -> "Local only"
            SyncStatusUiModel.QUEUED -> "Queued"
            SyncStatusUiModel.TOMBSTONED -> "Archived"
            SyncStatusUiModel.UNKNOWN -> "Unknown"
        }

    Row(
        modifier =
            modifier
                .background(semantic.container, RoundedCornerShape(AppDesignTokens.radius.pill))
                .padding(horizontal = AppDesignTokens.spacing.sm, vertical = AppDesignTokens.spacing.xs),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = semantic.onContainer,
        )
    }
}

@Composable
fun SyncStatusPill(
    status: String,
    modifier: Modifier = Modifier,
) {
    SyncStatusPill(status = SyncStatusUiModel.fromRaw(status), modifier = modifier)
}
