package com.personal.lifeOS.feature.finance.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
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
import com.personal.lifeOS.ui.theme.Warning
import com.personal.lifeOS.ui.theme.AppSpacing
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
@Suppress("LongMethod", "CyclomaticComplexMethod")
fun FinanceScreen(
    viewModel: FinanceViewModel = hiltViewModel(),
    onOpenTools: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsState()
    val lazyItems = viewModel.pagedTransactions.collectAsLazyPagingItems()
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

    val dayLabel by remember(lazyItems.itemCount) {
        derivedStateOf {
            val idx = txListState.firstVisibleItemIndex
            if (lazyItems.itemCount > 0 && idx < lazyItems.itemCount) {
                lazyItems[idx]?.date?.let { epochMillis ->
                    val date =
                        Instant.ofEpochMilli(epochMillis)
                            .atZone(ZoneId.systemDefault()).toLocalDate()
                    val today = LocalDate.now()
                    when {
                        date == today -> "Today"
                        date == today.minusDays(1) -> "Yesterday"
                        else -> date.format(DateTimeFormatter.ofPattern("MMM d"))
                    }
                } ?: ""
            } else {
                ""
            }
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
                    modifier = Modifier.size(24.dp),
                )
            }
            IconButton(onClick = { viewModel.onEvent(FinanceUiEvent.ShowImportDialog) }) {
                Icon(
                    imageVector = Icons.Filled.FileDownload,
                    contentDescription = "Import from SMS",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp),
                )
            }
        },
        contentPadding = PaddingValues(bottom = AppSpacing.BottomSafeWithFloatingNav),
    ) {
        if (uiState.isLoading) {
            LoadingState(label = "Loading finance data...")
            return@PageScaffold
        }

        if (uiState.enhancedUiEnabled) {
            FinanceImportBanner(
                uiState = uiState,
                onReview = { viewModel.onEvent(FinanceUiEvent.ShowImportDialog) },
            )
        }

        FinanceSummaryStrip(uiState = uiState)
        uiState.budgetGuardrail?.let { guardrail ->
            InlineBanner(
                message = "${guardrail.title} · ${guardrail.message}",
                tone = InlineBannerTone.WARNING,
            )
        }
        FinanceBudgetPressure(uiState = uiState)

        if (uiState.showFulizaBanner) {
            FulizaSummaryCard(
                outstanding = uiState.fulizaNetOutstandingKes ?: 0.0,
                openCount = uiState.fulizaOpenCount,
            )
        }

        SpendingVelocityBanner(
            monthSpend = uiState.summary.monthTotal,
            monthBudget = uiState.totalMonthBudget,
        )

        if (uiState.enhancedUiEnabled) {
            uiState.reviewQueueSummary?.let { summary ->
                ReviewQueueCard(
                    summary = summary,
                    onOpenReview = { viewModel.onEvent(FinanceUiEvent.ShowImportDialog) },
                )
            }
            uiState.exportNudge?.let { exportNudge ->
                FinanceActionNudgeCard(
                    nudge = exportNudge,
                    onOpenTools = onOpenTools,
                )
            }
        }

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
            onValueChange = { newQuery ->
                query = newQuery
                viewModel.onEvent(FinanceUiEvent.UpdateSearchQuery(newQuery))
            },
            placeholder = "Search merchant or category",
        )

        if (lazyItems.itemCount == 0 && lazyItems.loadState.refresh !is LoadState.Loading) {
            EmptyState(
                title = if (query.isBlank()) "No transactions yet" else "No matching transactions",
                description =
                    if (query.isBlank()) {
                        "Import MPESA messages or add a transaction to start your ledger."
                    } else {
                        "Try another filter or refine your search."
                    },
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
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

                AppCard(elevated = false) {
                    LazyColumn(
                        state = txListState,
                        modifier = Modifier.height(300.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        contentPadding = PaddingValues(top = 6.dp, bottom = 4.dp),
                    ) {
                        items(
                            count = lazyItems.itemCount,
                            key = lazyItems.itemKey { it.id },
                        ) { index ->
                            val transaction = lazyItems[index]
                            if (transaction != null) {
                                FinanceTransactionRow(
                                    transaction = transaction,
                                    onRecategorize = {
                                        viewModel.onEvent(FinanceUiEvent.ShowCategoryPicker(transaction))
                                    },
                                    onDelete = { deleteTarget = transaction },
                                )
                            }
                        }
                        if (lazyItems.loadState.append is LoadState.Loading) {
                            item {
                                Box(
                                    modifier = Modifier.fillMaxWidth(),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

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
private fun FinanceImportBanner(
    uiState: FinanceUiState,
    onReview: () -> Unit,
) {
    val importSummary =
        "${uiState.importHealth.pendingReviewCount} pending · " +
            "${uiState.importHealth.duplicateCount} duplicates · " +
            "${uiState.importHealth.parseFailureCount} issues"
    AppCard(elevated = true) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Import health",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    uiState.freshness?.let { freshness ->
                        Text(
                            text = freshness.label,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                TextButton(onClick = onReview) {
                    Text("Review")
                }
            }
            Text(
                text = importSummary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            uiState.freshness?.supportingLabel?.let { supportingLabel ->
                Text(
                    text = supportingLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun FinanceSummaryStrip(uiState: FinanceUiState) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        FinanceSummaryCard(
            title = "Today",
            amount = uiState.summary.todayTotal,
            modifier = Modifier.width(168.dp),
        )
        FinanceSummaryCard(
            title = "This Week",
            amount = uiState.summary.weekTotal,
            modifier = Modifier.width(168.dp),
        )
        FinanceSummaryCard(
            title = "This Month",
            amount = uiState.summary.monthTotal,
            modifier = Modifier.width(168.dp),
        )
    }
}

@Composable
private fun FinanceBudgetPressure(uiState: FinanceUiState) {
    val budgetAmount = uiState.totalMonthBudget.takeIf { it > 0.0 } ?: (uiState.summary.monthTotal * 1.20).coerceAtLeast(1.0)
    AppCard(elevated = true) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Budget Pressure",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            BudgetProgressIndicator(
                spentAmount = uiState.summary.monthTotal,
                budgetAmount = budgetAmount,
            )
        }
    }
}

@Composable
private fun ReviewQueueCard(
    summary: String,
    onOpenReview: () -> Unit,
) {
    AppCard(elevated = false) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = "Review queue",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            TextButton(onClick = onOpenReview) {
                Text("Open")
            }
        }
    }
}

@Composable
private fun FinanceActionNudgeCard(
    nudge: FinanceActionNudgeUiModel,
    onOpenTools: () -> Unit,
) {
    AppCard(elevated = false) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = nudge.title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = nudge.summary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(onClick = onOpenTools) {
                Text(nudge.actionLabel)
            }
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
            Text(
                text = DateUtils.formatCurrency(transaction.amount),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun FulizaSummaryCard(
    outstanding: Double,
    openCount: Int,
) {
    AppCard(elevated = true) {
        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = "Fuliza Outstanding",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "$openCount open loan${if (openCount != 1) "s" else ""}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = DateUtils.formatCurrency(outstanding),
                    style = MaterialTheme.typography.titleMedium,
                    color = Warning,
                    fontWeight = FontWeight.Bold,
                )
            }
            Text(
                text = "Pay to avoid daily interest charges",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SpendingVelocityBanner(monthSpend: Double, monthBudget: Double) {
    if (monthBudget <= 0.0) return
    val dayOfMonth = LocalDate.now().dayOfMonth
    if (dayOfMonth < 3) return
    val dailyRate = monthSpend / dayOfMonth
    val daysInMonth = LocalDate.now().lengthOfMonth()
    val projected = dailyRate * daysInMonth
    val overshoot = projected - monthBudget
    if (overshoot <= 0.0) return
    InlineBanner(
        message =
            "At this pace: ${DateUtils.formatCurrency(projected)} projected — " +
                "${DateUtils.formatCurrency(overshoot)} over budget",
        tone = InlineBannerTone.WARNING,
    )
}
