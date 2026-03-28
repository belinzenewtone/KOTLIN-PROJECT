package com.personal.lifeOS.features.recurring.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.personal.lifeOS.core.ui.designsystem.PageScaffold
import com.personal.lifeOS.ui.theme.AppSpacing

@Composable
fun RecurringScreen(viewModel: RecurringViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()

    PageScaffold(
        title = "Recurring",
        subtitle = "Track repeating tasks, subscriptions, and cashflow cycles",
        contentPadding = PaddingValues(bottom = AppSpacing.BottomSafeWithFloatingNav),
        actions = {
            IconButton(onClick = viewModel::showAddDialog) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "Add recurring rule",
                )
            }
        },
    ) {
        if (state.rules.isEmpty()) {
            RecurringEmptyStateCard()
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.Section),
            ) {
                state.rules.forEach { rule ->
                    RecurringRuleCard(
                        rule = rule,
                        onToggle = { enabled -> viewModel.toggleEnabled(rule, enabled) },
                        onDelete = { viewModel.deleteRule(rule.id) },
                    )
                }
            }
        }
    }

    if (state.showDialog) {
        AddRecurringRuleDialog(
            state = state,
            onDismiss = viewModel::hideDialog,
            onSetTitle = viewModel::setTitle,
            onSetAmount = viewModel::setAmount,
            onSetType = viewModel::setType,
            onSetCadence = viewModel::setCadence,
            onSave = viewModel::saveRule,
        )
    }
}
