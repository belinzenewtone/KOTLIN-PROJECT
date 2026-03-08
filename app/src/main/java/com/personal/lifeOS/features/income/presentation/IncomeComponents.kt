package com.personal.lifeOS.features.income.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import com.personal.lifeOS.features.income.domain.model.IncomeRecord
import com.personal.lifeOS.ui.components.GlassCard
import com.personal.lifeOS.ui.theme.Error
import com.personal.lifeOS.ui.theme.TextSecondary

@Composable
internal fun IncomeHeader(
    monthTotal: Double,
    onAdd: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text(
                text = "Income",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                text = "This month: ${DateUtils.formatCurrency(monthTotal)}",
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary,
            )
        }
        FloatingActionButton(
            onClick = onAdd,
            modifier = Modifier.size(48.dp),
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Add income")
        }
    }
}

@Composable
internal fun IncomeEmptyStateCard() {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "No income entries yet. Add salary, freelance, or side income streams.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
        )
    }
}

@Composable
internal fun IncomeCard(
    item: IncomeRecord,
    onDelete: () -> Unit,
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(item.source, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = DateUtils.formatDate(item.date, "MMM dd, yyyy"),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                )
                if (item.note.isNotBlank()) {
                    Text(item.note, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = DateUtils.formatCurrency(item.amount),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete income")
                }
            }
        }
    }
}

@Composable
internal fun AddIncomeDialog(
    state: IncomeUiState,
    onDismiss: () -> Unit,
    onSetSource: (String) -> Unit,
    onSetAmount: (String) -> Unit,
    onSetNote: (String) -> Unit,
    onSave: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Income") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = state.sourceInput,
                    onValueChange = onSetSource,
                    label = { Text("Source") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = state.amountInput,
                    onValueChange = onSetAmount,
                    label = { Text("Amount (KES)") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = state.noteInput,
                    onValueChange = onSetNote,
                    label = { Text("Note (optional)") },
                )
                state.error?.let {
                    Text(it, color = Error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = { TextButton(onClick = onSave) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
