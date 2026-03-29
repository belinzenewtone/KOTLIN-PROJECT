package com.personal.lifeOS.core.ui.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun SegmentedControl(
    items: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .background(
                    color = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.78f),
                    shape = RoundedCornerShape(AppDesignTokens.radius.lg),
                )
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f),
                    shape = RoundedCornerShape(AppDesignTokens.radius.lg),
                )
                .padding(AppDesignTokens.spacing.xs),
        horizontalArrangement = Arrangement.spacedBy(AppDesignTokens.spacing.xs),
    ) {
        items.forEachIndexed { index, label ->
            val selected = selectedIndex == index
            Row(
                modifier =
                    Modifier
                        .weight(1f)
                        .background(
                            color =
                                if (selected) {
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.92f)
                                } else {
                                    Color.Transparent
                                },
                            shape = RoundedCornerShape(AppDesignTokens.radius.md),
                        )
                        .clickable { onSelected(index) }
                        .padding(vertical = 10.dp, horizontal = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    color =
                        if (selected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                )
            }
        }
    }
}
