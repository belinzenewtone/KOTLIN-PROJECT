package com.personal.lifeOS.features.budget.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.personal.lifeOS.core.ui.designsystem.LoadingState
import com.personal.lifeOS.core.ui.designsystem.PageScaffold
import com.personal.lifeOS.core.ui.designsystem.TopBanner
import com.personal.lifeOS.core.ui.designsystem.TopBannerTone
import com.personal.lifeOS.ui.theme.AppSpacing

@Composable
fun BudgetScreen(
    viewModel: BudgetViewModel = hiltViewModel(),
    onBack: (() -> Unit)? = null,
) {
    val state by viewModel.uiState.collectAsState()

    PageScaffold(
        title = "Budgets",
        subtitle = "${state.budgets.size} ${if (state.budgets.size == 1) "category" else "categories"} tracked",
        onBack = onBack,
        topBanner = {
            state.error?.let {
                TopBanner(message = it, tone = TopBannerTone.ERROR)
            }
        },
        actions = {
            androidx.compose.material3.TextButton(onClick = viewModel::showAddDialog) {
                androidx.compose.material3.Text("Add budget")
            }
        },
        contentPadding = PaddingValues(bottom = AppSpacing.BottomSafeWithFloatingNav),
    ) {
        if (state.isLoading) {
            LoadingState(label = "Loading budgets...")
            return@PageScaffold
        }

        if (state.budgets.isEmpty()) {
            BudgetEmptyStateCard()
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Overall monthly summary card at top
                BudgetMonthSummaryCard(budgets = state.budgets)

                // Per-category budget cards
                state.budgets.forEach { progress ->
                    BudgetCard(
                        progress = progress,
                        onDelete = { viewModel.deleteBudget(progress.budget.id) },
                        onEdit = { viewModel.editBudget(progress) },
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
