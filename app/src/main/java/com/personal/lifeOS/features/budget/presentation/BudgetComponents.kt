package com.personal.lifeOS.features.budget.presentation

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.personal.lifeOS.core.ui.designsystem.AppCard
import com.personal.lifeOS.core.ui.designsystem.BudgetProgressIndicator
import com.personal.lifeOS.core.ui.designsystem.EmptyState
import com.personal.lifeOS.core.utils.DateUtils
import com.personal.lifeOS.features.budget.domain.model.BudgetPeriod
import com.personal.lifeOS.features.budget.domain.model.BudgetProgress
import com.personal.lifeOS.ui.theme.Error

@Composable
internal fun BudgetEmptyStateCard() {
    EmptyState(
        title = "No budgets yet",
        description = "Add one to track category limits against monthly spending.",
    )
}

@Composable
internal fun BudgetCard(
    progress: BudgetProgress,
    onDelete: () -> Unit,
) {
    AppCard(
        modifier = Modifier.fillMaxWidth(),
        elevated = true,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(progress.budget.category, style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "${progress.budget.period.name.lowercase().replaceFirstChar { it.uppercase() }} budget",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete budget")
                }
            }

            BudgetProgressIndicator(
                spentAmount = progress.spentAmount,
                budgetAmount = progress.budget.limitAmount,
            )

            Text(
                text = "Spent ${DateUtils.formatCurrency(
                    progress.spentAmount,
                )} of ${DateUtils.formatCurrency(progress.budget.limitAmount)}",
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = "Remaining ${DateUtils.formatCurrency(
                    progress.remainingAmount,
                )} • ${progress.usagePercent.toInt()}%",
                style = MaterialTheme.typography.labelMedium,
                color = if (progress.usagePercent >= 100f) Error else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
internal fun AddBudgetDialog(
    state: BudgetUiState,
    onDismiss: () -> Unit,
    onSetCategory: (String) -> Unit,
    onSetLimit: (String) -> Unit,
    onSetPeriod: (BudgetPeriod) -> Unit,
    onSave: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Budget") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = state.categoryInput,
                    onValueChange = onSetCategory,
                    label = { Text("Category") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = state.limitInput,
                    onValueChange = onSetLimit,
                    label = { Text("Limit Amount (KES)") },
                    singleLine = true,
                )

                Text("Period", style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    BudgetPeriod.entries.forEach { period ->
                        OutlinedButton(onClick = { onSetPeriod(period) }) {
                            val marker = if (state.periodInput == period) "• " else ""
                            Text("$marker${period.name.lowercase().replaceFirstChar { it.uppercase() }}")
                        }
                    }
                }

                state.error?.let {
                    Text(it, color = Error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = { TextButton(onClick = onSave) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
