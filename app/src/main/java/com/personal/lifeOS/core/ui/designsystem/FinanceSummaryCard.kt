package com.personal.lifeOS.core.ui.designsystem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import com.personal.lifeOS.core.utils.DateUtils

@Composable
fun FinanceSummaryCard(
    title: String,
    amount: Double,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
) {
    AppCard(
        modifier = modifier.fillMaxWidth(),
        elevated = true,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(AppDesignTokens.spacing.xs),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = DateUtils.formatCurrency(amount),
                style = MaterialTheme.typography.titleLarge.copy(fontFamily = FontFamily.Monospace),
                color = MaterialTheme.colorScheme.onSurface,
            )
            subtitle?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
