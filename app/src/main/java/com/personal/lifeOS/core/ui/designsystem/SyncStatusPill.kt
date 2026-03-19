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

@Composable
fun SyncStatusPill(
    status: String,
    modifier: Modifier = Modifier,
) {
    val (bg, fg) =
        when (status.uppercase()) {
            "SYNCED" -> Color(0xFFE8F5E9) to Color(0xFF2E7D32)
            "SYNCING" -> Color(0xFFE3F2FD) to Color(0xFF1565C0)
            "FAILED" -> Color(0xFFFFEBEE) to Color(0xFFC62828)
            "CONFLICT" -> Color(0xFFFFF8E1) to Color(0xFFEF6C00)
            else -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
        }
    Row(
        modifier =
            modifier
                .background(bg, RoundedCornerShape(999.dp))
                .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(
            text = status,
            style = MaterialTheme.typography.labelSmall,
            color = fg,
        )
    }
}
