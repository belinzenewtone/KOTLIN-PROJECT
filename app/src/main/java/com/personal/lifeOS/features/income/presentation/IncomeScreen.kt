package com.personal.lifeOS.features.income.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
fun IncomeScreen(viewModel: IncomeViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()

    PageScaffold(
        title = "Income",
        subtitle = "${state.records.size} entries tracked",
        actions = {
            TextButton(onClick = viewModel::showAddDialog) {
                Text("Add")
            }
        },
    ) {
        state.error?.let {
            InlineBanner(
                message = it,
                tone = InlineBannerTone.ERROR,
            )
        }

        if (state.isLoading) {
            LoadingState(label = "Loading income...")
            return@PageScaffold
        }

        IncomeSummaryCard(monthTotal = state.monthTotal)

        if (state.records.isEmpty()) {
            IncomeEmptyStateCard()
            return@PageScaffold
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            state.records.forEach { item ->
                IncomeCard(
                    item = item,
                    onDelete = { viewModel.deleteIncome(item.id) },
                )
            }
        }
    }

    if (state.showDialog) {
        AddIncomeDialog(
            state = state,
            onDismiss = viewModel::hideDialog,
            onSetSource = viewModel::setSource,
            onSetAmount = viewModel::setAmount,
            onSetNote = viewModel::setNote,
            onSave = viewModel::saveIncome,
        )
    }
}
