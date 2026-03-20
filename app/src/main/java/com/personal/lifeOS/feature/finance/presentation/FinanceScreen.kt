package com.personal.lifeOS.feature.finance.presentation

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
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
import com.personal.lifeOS.core.ui.designsystem.TopBanner
import com.personal.lifeOS.core.ui.designsystem.TopBannerTone
import com.personal.lifeOS.core.utils.DateUtils
import com.personal.lifeOS.feature.finance.domain.model.FinanceTransaction
import com.personal.lifeOS.feature.finance.domain.model.FinanceTransactionFilter

@Composable
@Suppress("LongMethod", "CyclomaticComplexMethod")
fun FinanceScreen(
    viewModel: FinanceViewModel = hiltViewModel(),
    onOpenTools: () -> Unit = {}, // kept for nav compatibility, no longer shown as button
) {
    val uiState by viewModel.uiState.collectAsState()
    var query by rememberSaveable { mutableStateOf("") }
    var deleteTarget by remember { mutableStateOf<FinanceTransaction?>(null) }
    val txListState = rememberLazyListState()

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

    // Day label tracks the topmost visible transaction as the user scrolls
    val dayLabel by remember(filteredTransactions) {
        derivedStateOf {
            filteredTransactions.getOrNull(txListState.firstVisibleItemIndex)
                ?.date
                ?.let { epochMillis ->
                    val date = Instant.ofEpochMilli(epochMillis)
                        .atZone(ZoneId.systemDefault()).toLocalDate()
                    val today = LocalDate.now()
                    when {
                        date == today -> "Today"
                        date == today.minusDays(1) -> "Yesterday"
                        else -> date.format(DateTimeFormatter.ofPattern("MMM d"))
                    }
                } ?: ""
        }
    }

    PageScaffold(
        title = "Finance",
        subtitle = "Ledger, imports, and spending health",
        topBanner = {
            uiState.errorMessage?.let {
                TopBanner(
                    message = it,
                    tone = TopBannerTone.ERROR,
                )
            } ?: run {
                uiState.importResultMessage?.takeIf { it.isNotBlank() }?.let {
                    TopBanner(
                        message = it,
                        tone = TopBannerTone.SUCCESS,
                    )
                }
            }
        },
        actions = {
            IconButton(onClick = { viewModel.onEvent(FinanceUiEvent.ShowAddDialog) }) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "Add transaction",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = androidx.compose.ui.Modifier.size(24.dp),
                )
            }
            IconButton(onClick = { viewModel.onEvent(FinanceUiEvent.ShowImportDialog) }) {
                Icon(
                    imageVector = Icons.Filled.FileDownload,
                    contentDescription = "Import from SMS",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = androidx.compose.ui.Modifier.size(24.dp),
                )
            }
        },
        contentPadding = PaddingValues(bottom = 140.dp),
    ) {
        if (uiState.isLoading) {
            LoadingState(label = "Loading finance data...")
            return@PageScaffold
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
            // ── Scrollable transactions box: shows 4 rows at a time ──────────
            AppCard(elevated = false) {
                // Header: label + day indicator
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Transactions",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (dayLabel.isNotBlank()) {
                        Text(
                            text = dayLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }

                LazyColumn(
                    state = txListState,
                    modifier = Modifier.height(300.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    contentPadding = PaddingValues(top = 6.dp, bottom = 4.dp),
                ) {
                    items(filteredTransactions, key = { it.id }) { transaction ->
                        FinanceTransactionRow(
                            transaction = transaction,
                            onRecategorize = {
                                viewModel.onEvent(FinanceUiEvent.ShowCategoryPicker(transaction))
                            },
                            onDelete = { deleteTarget = transaction },
                        )
                    }
                }
            }
        }
    }

    // ── Delete confirmation dialog ────────────────────────────────────────────
    deleteTarget?.let { tx ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Delete transaction?") },
            text = {
                Text(
                    "Remove \"${tx.merchant}\" (${DateUtils.formatCurrency(tx.amount)})? " +
                        "This cannot be undone.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.onEvent(FinanceUiEvent.DeleteTransaction(tx))
                    deleteTarget = null
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("Cancel") }
            },
        )
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
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Left: merchant + category/date (takes only the space it needs, not full width)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(1.dp),
            ) {
                Text(
                    text = transaction.merchant,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                )
                Text(
                    text = "${transaction.category} · ${DateUtils.formatDate(transaction.date, "MMM d, h:mm a")}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(0.dp)) {
                    TextButton(
                        onClick = onRecategorize,
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                    ) {
                        Text("Category", style = MaterialTheme.typography.labelSmall)
                    }
                    TextButton(
                        onClick = onDelete,
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                    ) {
                        Text(
                            "Delete",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
            // Right: amount
            Text(
                text = DateUtils.formatCurrency(transaction.amount),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}
