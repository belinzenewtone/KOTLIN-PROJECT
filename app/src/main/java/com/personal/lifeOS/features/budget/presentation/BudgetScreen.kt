package com.personal.lifeOS.features.budget.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.personal.lifeOS.core.ui.designsystem.InlineBanner
import com.personal.lifeOS.core.ui.designsystem.InlineBannerTone
import com.personal.lifeOS.core.ui.designsystem.LoadingState
import com.personal.lifeOS.core.ui.designsystem.PageScaffold

@Composable
fun BudgetScreen(viewModel: BudgetViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()

    PageScaffold(
        title = "Budgets",
        subtitle = "${state.budgets.size} categories tracked",
        actions = {
            androidx.compose.material3.TextButton(onClick = viewModel::showAddDialog) {
                androidx.compose.material3.Text("Add")
            }
        },
        contentPadding = PaddingValues(bottom = 140.dp),
    ) {
        state.error?.let {
            InlineBanner(
                message = it,
                tone = InlineBannerTone.ERROR,
            )
        }

        if (state.isLoading) {
            LoadingState(label = "Loading budgets...")
            return@PageScaffold
        }

        if (state.budgets.isEmpty()) {
            BudgetEmptyStateCard()
        } else {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                state.budgets.forEach { progress ->
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
