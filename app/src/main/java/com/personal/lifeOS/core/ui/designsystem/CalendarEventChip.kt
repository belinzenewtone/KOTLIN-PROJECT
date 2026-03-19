package com.personal.lifeOS.core.ui.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun CalendarEventChip(
    title: String,
    timeLabel: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(AppDesignTokens.radius.md),
                )
                .padding(horizontal = AppDesignTokens.spacing.md, vertical = AppDesignTokens.spacing.sm),
        horizontalArrangement = Arrangement.spacedBy(AppDesignTokens.spacing.sm),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = "•",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = timeLabel,
            style = MaterialTheme.typography.labelMedium,
            color = AppDesignTokens.colors.primary,
        )
    }
}
