package com.personal.lifeOS.features.income.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.personal.lifeOS.core.ui.designsystem.InlineBanner
import com.personal.lifeOS.core.ui.designsystem.InlineBannerTone
import com.personal.lifeOS.core.ui.designsystem.LoadingState
import com.personal.lifeOS.core.ui.designsystem.PageScaffold
import com.personal.lifeOS.ui.theme.AppSpacing

@Composable
fun IncomeScreen(
    viewModel: IncomeViewModel = hiltViewModel(),
    onBack: (() -> Unit)? = null,
) {
    val state by viewModel.uiState.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        PageScaffold(
            headerEyebrow = "Cash Flow",
            title = "Income",
            subtitle = "${state.records.size} entries tracked",
            onBack = onBack,
            contentPadding = PaddingValues(bottom = AppSpacing.BottomSafeWithFloatingNav),
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
                    imageVector = Icons.Outlined.Add,
                    contentDescription = "Add income",
                )
            },
            text = { Text("Add income") },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            elevation = FloatingActionButtonDefaults.elevation(
                defaultElevation = 6.dp,
                pressedElevation = 10.dp,
            ),
        )
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
