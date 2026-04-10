package com.personal.lifeOS.features.budget.presentation

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.personal.lifeOS.core.ui.designsystem.AppCard
import com.personal.lifeOS.core.ui.designsystem.AppDesignTokens
import com.personal.lifeOS.core.ui.designsystem.EmptyState
import com.personal.lifeOS.core.utils.DateUtils
import com.personal.lifeOS.features.budget.domain.model.BudgetPeriod
import com.personal.lifeOS.features.budget.domain.model.BudgetProgress
import com.personal.lifeOS.ui.theme.Error
import com.personal.lifeOS.ui.theme.Success
import com.personal.lifeOS.ui.theme.Warning

// Predefined expense categories (matches the app's transaction categories)
internal val BUDGET_CATEGORIES = listOf(
    "Food", "Transport", "Utilities", "Entertainment", "Shopping",
    "Health", "Education", "Housing", "Airtime", "Savings",
    "Personal Care", "Subscriptions", "Fuliza", "Other",
)

@Composable
internal fun BudgetEmptyStateCard() {
    EmptyState(
        title = "No budgets yet",
        description = "Tap + to set a monthly limit for any spending category.",
    )
}

/** Color-coded status for budget usage */
private fun budgetStatusColor(ratio: Float): Color = when {
    ratio >= 1.0f -> Error
    ratio >= 0.8f -> Warning
    else -> Success
}

private fun budgetStatusLabel(spentAmount: Double, budgetAmount: Double, ratio: Float): String = when {
    ratio >= 1.0f -> "Over by ${DateUtils.formatCurrency(spentAmount - budgetAmount)}"
    ratio >= 0.8f -> "Near limit"
    else -> "On track"
}

@Composable
internal fun BudgetMonthSummaryCard(budgets: List<BudgetProgress>) {
    if (budgets.isEmpty()) return
    val totalBudget = budgets.sumOf { it.budget.limitAmount }
    val totalSpent = budgets.sumOf { it.spentAmount }
    val overallRatio = if (totalBudget > 0) (totalSpent / totalBudget).toFloat().coerceAtLeast(0f) else 0f
    val statusColor = budgetStatusColor(overallRatio)

    AppCard(elevated = true) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = "This Month",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "${budgets.size} categories",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = DateUtils.formatCurrency(totalSpent),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = statusColor,
                    )
                    Text(
                        text = "of ${DateUtils.formatCurrency(totalBudget)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // Overall usage bar
            val animatedRatio by animateFloatAsState(
                targetValue = overallRatio.coerceIn(0f, 1f),
                animationSpec = tween(600),
                label = "overall_budget_ratio",
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(animatedRatio)
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(statusColor),
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = budgetStatusLabel(totalSpent, totalBudget, overallRatio),
                        style = MaterialTheme.typography.labelSmall,
                        color = statusColor,
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        text = "${(overallRatio * 100).toInt()}% used",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
internal fun BudgetCard(
    progress: BudgetProgress,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
) {
    val ratio = if (progress.budget.limitAmount > 0)
        (progress.spentAmount / progress.budget.limitAmount).toFloat().coerceAtLeast(0f)
    else 0f
    val statusColor = budgetStatusColor(ratio)
    val animatedRatio by animateFloatAsState(
        targetValue = ratio.coerceIn(0f, 1f),
        animationSpec = tween(500),
        label = "budget_ratio_${progress.budget.id}",
    )

    AppCard(modifier = Modifier.fillMaxWidth(), elevated = true) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Header row: category + percentage badge + actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    // Colored status dot
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(statusColor, CircleShape),
                    )
                    Column {
                        Text(
                            text = progress.budget.category
                                .lowercase()
                                .replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = progress.budget.period.name
                                .lowercase()
                                .replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Usage percentage badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(AppDesignTokens.radius.lg))
                            .background(statusColor.copy(alpha = 0.15f))
                            .padding(horizontal = 10.dp, vertical = 3.dp),
                    ) {
                        Text(
                            text = "${(ratio * 100).toInt()}%",
                            style = MaterialTheme.typography.labelMedium,
                            color = statusColor,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Outlined.Edit, contentDescription = "Edit", modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(
                            Icons.Outlined.Delete,
                            contentDescription = "Delete",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }

            // Animated progress bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedRatio)
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(statusColor),
                )
            }

            // Amounts row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text(
                        text = DateUtils.formatCurrency(progress.spentAmount),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = statusColor,
                    )
                    Text(
                        text = "spent",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = budgetStatusLabel(progress.spentAmount, progress.budget.limitAmount, ratio),
                        style = MaterialTheme.typography.labelSmall,
                        color = statusColor,
                        fontWeight = FontWeight.Medium,
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = DateUtils.formatCurrency(progress.remainingAmount.coerceAtLeast(0.0)),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        text = "remaining",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AddBudgetDialog(
    state: BudgetUiState,
    onDismiss: () -> Unit,
    onSetCategory: (String) -> Unit,
    onSetLimit: (String) -> Unit,
    onSetPeriod: (BudgetPeriod) -> Unit,
    onSave: () -> Unit,
) {
    var dropdownExpanded by remember { mutableStateOf(false) }
    val isEditing = state.editingBudgetId != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEditing) "Edit Budget" else "Set Budget") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.verticalScroll(rememberScrollState()),
            ) {
                // Category dropdown
                ExposedDropdownMenuBox(
                    expanded = dropdownExpanded,
                    onExpandedChange = { dropdownExpanded = it },
                ) {
                    OutlinedTextField(
                        value = state.categoryInput
                            .lowercase()
                            .replaceFirstChar { it.uppercase() },
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        singleLine = true,
                    )
                    ExposedDropdownMenu(
                        expanded = dropdownExpanded,
                        onDismissRequest = { dropdownExpanded = false },
                    ) {
                        BUDGET_CATEGORIES.forEach { category ->
                            DropdownMenuItem(
                                text = { Text(category) },
                                onClick = {
                                    onSetCategory(category.uppercase())
                                    dropdownExpanded = false
                                },
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = state.limitInput,
                    onValueChange = onSetLimit,
                    label = { Text("Monthly Limit (KES)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    prefix = { Text("KES ") },
                )

                state.limitInput.toDoubleOrNull()?.let { amount ->
                    if (amount > 0) {
                        Text(
                            text = "Daily equivalent: ${DateUtils.formatCurrency(amount / 30)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                state.error?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = onSave) {
                Text(if (isEditing) "Update" else "Set Budget")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
