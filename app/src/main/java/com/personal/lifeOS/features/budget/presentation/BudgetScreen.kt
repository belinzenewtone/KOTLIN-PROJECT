package com.personal.lifeOS.features.budget.presentation

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
fun BudgetScreen(viewModel: BudgetViewModel = hiltViewModel()) {
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
        BudgetHeader(onAdd = viewModel::showAddDialog)

        if (state.budgets.isEmpty()) {
            BudgetEmptyStateCard()
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.Section),
            ) {
                items(state.budgets, key = { it.budget.id }) { progress ->
                    BudgetCard(
                        progress = progress,
                        onDelete = { viewModel.deleteBudget(progress.budget.id) },
                    )
                }
            }
        }
    }

    if (state.showDialog) {
        AddBudgetDialog(
            state = state,
            onDismiss = viewModel::hideDialog,
            onSetCategory = viewModel::setCategory,
            onSetLimit = viewModel::setLimit,
            onSetPeriod = viewModel::setPeriod,
            onSave = viewModel::saveBudget,
        )
    }
}
