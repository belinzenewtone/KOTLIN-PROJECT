package com.personal.lifeOS.features.recurring.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.personal.lifeOS.ui.theme.AppSpacing
import com.personal.lifeOS.ui.theme.BackgroundDark

@Composable
fun RecurringScreen(viewModel: RecurringViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(BackgroundDark)
                .padding(horizontal = AppSpacing.ScreenHorizontal)
                .padding(top = AppSpacing.ScreenTop, bottom = AppSpacing.Section),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.Section),
    ) {
        RecurringHeader(onAdd = viewModel::showAddDialog)

        if (state.rules.isEmpty()) {
            RecurringEmptyStateCard()
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.Section),
            ) {
                items(state.rules, key = { it.id }) { rule ->
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
