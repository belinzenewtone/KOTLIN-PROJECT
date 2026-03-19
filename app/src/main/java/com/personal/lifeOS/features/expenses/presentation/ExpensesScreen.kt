package com.personal.lifeOS.features.expenses.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import com.personal.lifeOS.ui.components.StyledSnackbarHost
import com.personal.lifeOS.ui.theme.AppSpacing
import com.personal.lifeOS.ui.theme.BackgroundDark
import com.personal.lifeOS.ui.theme.Primary
import com.personal.lifeOS.ui.theme.TextPrimary

@Composable
fun ExpensesScreen(viewModel: ExpensesViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.error) {
        state.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        containerColor = BackgroundDark,
        snackbarHost = { StyledSnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                modifier =
                    Modifier
                        .navigationBarsPadding()
                        .padding(bottom = AppSpacing.FabBottomOffset),
                onClick = { viewModel.showAddDialog() },
                containerColor = Primary,
                contentColor = TextPrimary,
                shape = CircleShape,
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add transaction")
            }
        },
    ) { padding ->
        LazyColumn(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .statusBarsPadding()
                    .padding(horizontal = AppSpacing.ScreenHorizontal),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.Section),
            contentPadding = PaddingValues(bottom = AppSpacing.BottomSafeWithFab),
        ) {
            item {
                Spacer(Modifier.height(AppSpacing.ScreenTop))
                ExpensesHeader(onImportSms = { viewModel.showImportDialog() })
                Spacer(Modifier.height(AppSpacing.ScreenTop))
            }

            item { SpendingSummaryRow(summary = state.summary) }
            item { FilterChips(selected = state.selectedFilter, onSelect = { viewModel.setFilter(it) }) }

            item {
                CategoryBreakdownSection(
                    categories = state.summary.categoryBreakdown,
                    onCategoryClick = { viewModel.filterByCategory(it) },
                )
            }

            item {
                TransactionsHeader(
                    selectedCategory = state.selectedCategory,
                    onClearFilter = { viewModel.filterByCategory(null) },
                )
            }

            if (state.isLoading) {
                item { ExpensesLoadingState() }
            }

            if (!state.isLoading && state.transactions.isEmpty()) {
                item { ExpensesEmptyStateCard() }
            }

            items(state.transactions, key = { it.id }) { transaction ->
                TransactionItem(
                    transaction = transaction,
                    onDelete = { viewModel.delete(transaction) },
                    onRecategorize = { viewModel.showCategoryPicker(transaction) },
                )
            }
        }
    }

    if (state.showAddDialog) {
        AddTransactionDialog(
            onDismiss = { viewModel.hideAddDialog() },
            onAdd = { amount, merchant, category ->
                viewModel.addManualTransaction(amount, merchant, category)
            },
        )
    }

    state.showCategoryPicker?.let { tx ->
        CategoryPickerDialog(
            currentCategory = tx.category,
            onDismiss = { viewModel.hideCategoryPicker() },
            onSelect = { newCategory -> viewModel.recategorize(tx, newCategory) },
        )
    }

    if (state.showImportDialog) {
        val context = LocalContext.current
        SmsImportDialog(
            onDismiss = { viewModel.hideImportDialog() },
            onImportDays = { days ->
                viewModel.importSmsMessages(context.contentResolver, days)
            },
        )
    }
}
