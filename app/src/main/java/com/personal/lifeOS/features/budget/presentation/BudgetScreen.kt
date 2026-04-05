package com.personal.lifeOS.features.budget.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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

    Box(modifier = Modifier.fillMaxSize()) {
        PageScaffold(
            headerEyebrow = "Spending Guardrails",
            title = "Budgets",
            subtitle = "${state.budgets.size} ${if (state.budgets.size == 1) "category" else "categories"} tracked",
            onBack = onBack,
            topBanner = {
                state.error?.let {
                    TopBanner(message = it, tone = TopBannerTone.ERROR)
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

        ExtendedFloatingActionButton(
            onClick = viewModel::showAddDialog,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(
                    end = AppSpacing.ScreenHorizontal,
                    bottom = AppSpacing.BottomSafeWithFloatingNav + 8.dp,
                ),
            icon = {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "Add budget",
                )
            },
            text = { Text("Add budget") },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            elevation = FloatingActionButtonDefaults.elevation(
                defaultElevation = 6.dp,
                pressedElevation = 10.dp,
            ),
        )
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
