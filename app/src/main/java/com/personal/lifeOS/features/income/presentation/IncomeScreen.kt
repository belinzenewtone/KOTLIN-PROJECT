package com.personal.lifeOS.features.income.presentation

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
fun IncomeScreen(viewModel: IncomeViewModel = hiltViewModel()) {
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
        IncomeHeader(
            monthTotal = state.monthTotal,
            onAdd = viewModel::showAddDialog,
        )

        if (state.records.isEmpty()) {
            IncomeEmptyStateCard()
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.Section),
            ) {
                items(state.records, key = { it.id }) { item ->
                    IncomeCard(
                        item = item,
                        onDelete = { viewModel.deleteIncome(item.id) },
                    )
                }
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
