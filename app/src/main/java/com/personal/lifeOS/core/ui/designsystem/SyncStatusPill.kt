package com.personal.lifeOS.core.ui.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.personal.lifeOS.core.ui.model.SyncStatusUiModel

@Composable
fun SyncStatusPill(
    status: SyncStatusUiModel,
    modifier: Modifier = Modifier,
) {
    val (bg, fg) =
        when (status) {
            SyncStatusUiModel.SYNCED -> Color(0xFFE8F5E9) to Color(0xFF2E7D32)
            SyncStatusUiModel.SYNCING -> Color(0xFFE3F2FD) to Color(0xFF1565C0)
            SyncStatusUiModel.FAILED -> Color(0xFFFFEBEE) to Color(0xFFC62828)
            SyncStatusUiModel.CONFLICT -> Color(0xFFFFF8E1) to Color(0xFFEF6C00)
            SyncStatusUiModel.LOCAL_ONLY -> Color(0xFFEDE7F6) to Color(0xFF5E35B1)
            SyncStatusUiModel.QUEUED -> Color(0xFFE0F7FA) to Color(0xFF00838F)
            SyncStatusUiModel.TOMBSTONED -> Color(0xFFECEFF1) to Color(0xFF455A64)
            SyncStatusUiModel.UNKNOWN ->
                MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
        }
    Row(
        modifier =
            modifier
                .background(bg, RoundedCornerShape(999.dp))
                .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(
            text = status.name,
            style = MaterialTheme.typography.labelSmall,
            color = fg,
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
