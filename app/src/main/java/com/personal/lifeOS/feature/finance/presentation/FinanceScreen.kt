package com.personal.lifeOS.feature.finance.presentation

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.personal.lifeOS.core.ui.designsystem.AppCard
import com.personal.lifeOS.core.ui.designsystem.BudgetProgressIndicator
import com.personal.lifeOS.core.ui.designsystem.EmptyState
import com.personal.lifeOS.core.ui.designsystem.FinanceSummaryCard
import com.personal.lifeOS.core.ui.designsystem.ImportHealthPanel
import com.personal.lifeOS.core.ui.designsystem.InlineBanner
import com.personal.lifeOS.core.ui.designsystem.InlineBannerTone
import com.personal.lifeOS.core.ui.designsystem.LoadingState
import com.personal.lifeOS.core.ui.designsystem.PageScaffold
import com.personal.lifeOS.core.ui.designsystem.SearchField
import com.personal.lifeOS.core.ui.designsystem.SegmentedControl
import com.personal.lifeOS.core.utils.DateUtils
import com.personal.lifeOS.feature.finance.domain.model.FinanceTransaction
import com.personal.lifeOS.feature.finance.domain.model.FinanceTransactionFilter

@Composable
@Suppress("LongMethod", "CyclomaticComplexMethod")
fun FinanceScreen(
    viewModel: FinanceViewModel = hiltViewModel(),
    onOpenTools: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsState()
    var query by rememberSaveable { mutableStateOf("") }

    val filters = remember { listOf("All", "Today", "Week", "Month") }
    val selectedFilterIndex =
        when (uiState.selectedFilter) {
            FinanceTransactionFilter.ALL -> 0
            FinanceTransactionFilter.TODAY -> 1
            FinanceTransactionFilter.THIS_WEEK -> 2
            FinanceTransactionFilter.THIS_MONTH -> 3
        }

    val filteredTransactions =
        remember(uiState.recentTransactions, query) {
            if (query.isBlank()) {
                uiState.recentTransactions
            } else {
                val search = query.trim()
                uiState.recentTransactions.filter { tx ->
                    tx.merchant.contains(search, ignoreCase = true) ||
                        tx.category.contains(search, ignoreCase = true)
                }
            }
        }

    PageScaffold(
        title = "Finance",
        subtitle = "Ledger, imports, and spending health",
        actions = {
            TextButton(onClick = { viewModel.onEvent(FinanceUiEvent.ShowAddDialog) }) { Text("Add") }
            TextButton(onClick = { viewModel.onEvent(FinanceUiEvent.ShowImportDialog) }) { Text("Import") }
            TextButton(onClick = onOpenTools) { Text("Tools") }
        },
        contentPadding = PaddingValues(bottom = 140.dp),
    ) {
        if (uiState.isLoading) {
            LoadingState(label = "Loading finance data...")
            return@PageScaffold
        }

        uiState.errorMessage?.let {
            InlineBanner(
                message = it,
                tone = InlineBannerTone.ERROR,
            )
        }

        uiState.importResultMessage?.takeIf { it.isNotBlank() }?.let {
            InlineBanner(
                message = it,
                tone = InlineBannerTone.INFO,
            )
        }

        FinanceSummaryStrip(uiState = uiState)
        FinanceBudgetPressure(uiState = uiState)
        ImportHealthPanel(
            model = uiState.importHealth,
            onReview = { viewModel.onEvent(FinanceUiEvent.ShowImportDialog) },
        )

        SegmentedControl(
            items = filters,
            selectedIndex = selectedFilterIndex,
            onSelected = { index ->
                val filter =
                    when (index) {
                        1 -> FinanceTransactionFilter.TODAY
                        2 -> FinanceTransactionFilter.THIS_WEEK
                        3 -> FinanceTransactionFilter.THIS_MONTH
                        else -> FinanceTransactionFilter.ALL
                    }
                viewModel.onEvent(FinanceUiEvent.SelectFilter(filter))
            },
        )

        SearchField(
            value = query,
            onValueChange = { query = it },
            placeholder = "Search merchant or category",
        )

        if (filteredTransactions.isEmpty()) {
            EmptyState(
                title = "No matching transactions",
                description = "Try another filter or import MPESA messages.",
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                filteredTransactions.forEach { transaction ->
                    FinanceTransactionRow(
                        transaction = transaction,
                        onRecategorize = {
                            viewModel.onEvent(FinanceUiEvent.ShowCategoryPicker(transaction))
                        },
                        onDelete = {
                            viewModel.onEvent(FinanceUiEvent.DeleteTransaction(transaction))
                        },
                    )
                }
            }
        }
    }

    if (uiState.showAddDialog) {
        AddTransactionDialog(
            onDismiss = { viewModel.onEvent(FinanceUiEvent.HideAddDialog) },
            onAdd = { amount, merchant, category ->
                viewModel.onEvent(
                    FinanceUiEvent.AddManualTransaction(
                        amount = amount,
                        merchant = merchant,
                        category = category,
                    ),
                )
            },
        )
    }

    uiState.showCategoryPicker?.let { tx ->
        CategoryPickerDialog(
            currentCategory = tx.category,
            onDismiss = { viewModel.onEvent(FinanceUiEvent.HideCategoryPicker) },
            onSelect = { selected ->
                viewModel.onEvent(
                    FinanceUiEvent.RecategorizeTransaction(
                        transaction = tx,
                        category = selected,
                    ),
                )
            },
        )
    }

    if (uiState.showImportDialog) {
        SmsImportDialog(
            onDismiss = { viewModel.onEvent(FinanceUiEvent.HideImportDialog) },
            onImportDays = { days ->
                viewModel.onEvent(FinanceUiEvent.ImportSmsMessages(days))
            },
        )
    }
}

@Composable
private fun FinanceSummaryStrip(uiState: FinanceUiState) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        FinanceSummaryCard(
            title = "Today",
            amount = uiState.summary.todayTotal,
            modifier = Modifier.width(180.dp),
        )
        FinanceSummaryCard(
            title = "This Week",
            amount = uiState.summary.weekTotal,
            modifier = Modifier.width(180.dp),
        )
        FinanceSummaryCard(
            title = "This Month",
            amount = uiState.summary.monthTotal,
            modifier = Modifier.width(180.dp),
        )
    }
}

@Composable
private fun FinanceBudgetPressure(uiState: FinanceUiState) {
    val syntheticBudget = (uiState.summary.monthTotal * 1.20).coerceAtLeast(1.0)
    AppCard(elevated = true) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Budget Pressure",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            BudgetProgressIndicator(
                spentAmount = uiState.summary.monthTotal,
                budgetAmount = syntheticBudget,
            )
        }
    }
}

@Composable
private fun FinanceTransactionRow(
    transaction: FinanceTransaction,
    onRecategorize: () -> Unit,
    onDelete: () -> Unit,
) {
    AppCard(elevated = true) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = transaction.merchant,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "${transaction.category} • ${DateUtils.formatDate(transaction.date, "MMM dd, h:mm a")}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onRecategorize) { Text("Category") }
                    TextButton(onClick = onDelete) { Text("Delete") }
                }
            }
            Text(
                text = DateUtils.formatCurrency(transaction.amount),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}
