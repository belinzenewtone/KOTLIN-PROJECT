package com.personal.lifeOS.features.merchantdetail.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.personal.lifeOS.core.database.entity.TransactionEntity
import com.personal.lifeOS.core.ui.designsystem.AppCard
import com.personal.lifeOS.core.ui.designsystem.EmptyState
import com.personal.lifeOS.core.ui.designsystem.InlineBanner
import com.personal.lifeOS.core.ui.designsystem.InlineBannerTone
import com.personal.lifeOS.core.ui.designsystem.LoadingState
import com.personal.lifeOS.core.ui.designsystem.PageScaffold
import com.personal.lifeOS.core.utils.DateUtils
import com.personal.lifeOS.ui.theme.AppSpacing
import com.personal.lifeOS.ui.theme.Success

@Composable
fun MerchantDetailScreen(
    onBack: (() -> Unit)? = null,
    viewModel: MerchantDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    PageScaffold(
        headerEyebrow = "Merchant",
        title = state.merchantName.ifBlank { "Merchant" },
        subtitle = "${state.transactionCount} transaction${if (state.transactionCount != 1) "s" else ""}",
        onBack = onBack,
        contentPadding = PaddingValues(bottom = AppSpacing.BottomSafeWithFloatingNav),
    ) {
        state.error?.let {
            InlineBanner(message = it, tone = InlineBannerTone.ERROR)
        }

        if (state.isLoading) {
            LoadingState(label = "Loading merchant history…")
            return@PageScaffold
        }

        if (state.transactions.isEmpty()) {
            EmptyState(
                title = "No transactions",
                description = "No transactions found for this merchant.",
            )
            return@PageScaffold
        }

        // ── Stats summary card ──────────────────────────────────────────────
        AppCard(modifier = Modifier.fillMaxWidth(), elevated = true) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                MerchantStatColumn(
                    label = "Total Spend",
                    value = DateUtils.formatCurrency(state.totalSpend),
                )
                MerchantStatColumn(
                    label = "Transactions",
                    value = "${state.transactionCount}",
                )
                MerchantStatColumn(
                    label = "Avg. Amount",
                    value = DateUtils.formatCurrency(state.averageAmount),
                )
            }
        }

        // ── Transaction history list ────────────────────────────────────────
        Text(
            text = "Transaction History",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            state.transactions.forEach { tx ->
                MerchantTxRow(transaction = tx)
            }
        }
    }
}

@Composable
private fun MerchantStatColumn(label: String, value: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun MerchantTxRow(transaction: TransactionEntity) {
    val isIncome = transaction.transactionType.uppercase() in setOf("RECEIVED", "DEPOSIT")
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = transaction.category.lowercase().replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = DateUtils.formatDate(transaction.date, "MMM dd, yyyy · h:mm a"),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = if (isIncome) "+${DateUtils.formatCurrency(transaction.amount)}"
                   else DateUtils.formatCurrency(transaction.amount),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = if (isIncome) Success else MaterialTheme.colorScheme.onSurface,
        )
    }
}
