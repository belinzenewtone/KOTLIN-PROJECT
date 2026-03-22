package com.personal.lifeOS.core.ui.designsystem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.personal.lifeOS.core.utils.DateUtils

@Composable
fun BudgetProgressIndicator(
    spentAmount: Double,
    budgetAmount: Double,
    modifier: Modifier = Modifier,
) {
    val ratio = if (budgetAmount <= 0.0) 0f else (spentAmount / budgetAmount).toFloat().coerceIn(0f, 1f)
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AppDesignTokens.spacing.xs),
    ) {
        Text(
            text = "${DateUtils.formatCurrency(spentAmount)} of ${DateUtils.formatCurrency(budgetAmount)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        val barColor = when {
            ratio >= 1.0f -> MaterialTheme.colorScheme.error      // over budget — red
            ratio >= 0.8f -> AppDesignTokens.colors.warning       // near limit — orange
            else          -> AppDesignTokens.colors.success        // healthy — green
        }

        val statusLabel = when {
            ratio >= 1.0f -> {
                val over = spentAmount - budgetAmount
                "Over by ${DateUtils.formatCurrency(over)}"
            }
            ratio >= 0.8f -> "Near limit · ${(ratio * 100).toInt()}%"
            else -> null
        }

        LinearProgressIndicator(
            progress = { ratio },
            modifier = Modifier.fillMaxWidth(),
            color = barColor,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )
        statusLabel?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.labelSmall,
                color = barColor,
            )
        }
    }
}
