package com.personal.lifeOS.core.ui.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.personal.lifeOS.ui.theme.Error
import com.personal.lifeOS.ui.theme.Info
import com.personal.lifeOS.ui.theme.Warning

/**
 * Priority dot colors per the UI/UX guide:
 *   High / Urgent  → Red (#EF5350)
 *   Medium / Important → Amber (#FFB74D)
 *   Low / Neutral  → Blue (#42A5F5)
 */
private fun priorityDotColor(priority: String): Color =
    when (priority.uppercase()) {
        "URGENT", "HIGH" -> Error
        "IMPORTANT", "MEDIUM" -> Warning
        else -> Info
    }

@Composable
fun TaskRow(
    title: String,
    subtitle: String,
    isCompleted: Boolean,
    modifier: Modifier = Modifier,
    /** Pass the TaskPriority.name string, or "" to hide the dot. */
    priority: String = "",
    onToggleComplete: () -> Unit = {},
    onClick: () -> Unit = {},
) {
    AppCard(
        modifier =
            modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
        elevated = true,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppDesignTokens.spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Completion toggle circle
            Box(
                modifier =
                    Modifier
                        .size(22.dp)
                        .background(
                            color =
                                if (isCompleted) {
                                    AppDesignTokens.colors.primary
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                },
                            shape = CircleShape,
                        )
                        .clickable(onClick = onToggleComplete),
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.size(AppDesignTokens.spacing.xs))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // Priority dot — only shown when priority string is non-empty and task is pending
            if (priority.isNotBlank() && !isCompleted) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(
                            color = priorityDotColor(priority),
                            shape = CircleShape,
                        ),
                )
            }
        }
    }
}
