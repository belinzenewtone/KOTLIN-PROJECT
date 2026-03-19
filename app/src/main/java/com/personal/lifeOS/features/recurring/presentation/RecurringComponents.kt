package com.personal.lifeOS.features.recurring.presentation

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.personal.lifeOS.core.utils.DateUtils
import com.personal.lifeOS.features.recurring.domain.model.RecurringCadence
import com.personal.lifeOS.features.recurring.domain.model.RecurringRule
import com.personal.lifeOS.features.recurring.domain.model.RecurringType
import com.personal.lifeOS.ui.components.GlassCard
import com.personal.lifeOS.ui.theme.Error
import com.personal.lifeOS.ui.theme.TextSecondary

@Composable
internal fun RecurringHeader(onAdd: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Recurring",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.semantics { heading() },
        )
        FloatingActionButton(
            onClick = onAdd,
            modifier = Modifier.size(48.dp),
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Add recurring rule")
        }
    }
}

@Composable
internal fun RecurringEmptyStateCard() {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "No recurring rules yet. Add subscriptions, salary cycles, or recurring tasks.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
        )
    }
}

@Composable
internal fun RecurringRuleCard(
    rule: RecurringRule,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit,
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(rule.title, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "${rule.type.name} • ${rule.cadence.name}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                )
                val amountText = rule.amount?.let { DateUtils.formatCurrency(it) } ?: "No amount"
                Text(amountText, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                Text(
                    text = "Next run: ${DateUtils.formatDate(rule.nextRunAt, "MMM dd, yyyy")}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(
                    checked = rule.enabled,
                    onCheckedChange = onToggle,
                )
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete rule")
                }
            }
        }
    }
}

@Composable
internal fun AddRecurringRuleDialog(
    state: RecurringUiState,
    onDismiss: () -> Unit,
    onSetTitle: (String) -> Unit,
    onSetAmount: (String) -> Unit,
    onSetType: (RecurringType) -> Unit,
    onSetCadence: (RecurringCadence) -> Unit,
    onSave: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Recurring Rule") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = state.titleInput,
                    onValueChange = onSetTitle,
                    label = { Text("Title") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = state.amountInput,
                    onValueChange = onSetAmount,
                    label = { Text("Amount (optional for TASK)") },
                    singleLine = true,
                )

                Text("Type", style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    RecurringType.entries.forEach { type ->
                        OutlinedButton(onClick = { onSetType(type) }) {
                            val marker = if (state.typeInput == type) "• " else ""
                            Text("$marker${type.name}")
                        }
                    }
                }

                Text("Cadence", style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    RecurringCadence.entries.forEach { cadence ->
                        OutlinedButton(onClick = { onSetCadence(cadence) }) {
                            val marker = if (state.cadenceInput == cadence) "• " else ""
                            Text("$marker${cadence.name}")
                        }
                    }
                }

                state.error?.let {
                    Text(it, color = Error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onSave) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
