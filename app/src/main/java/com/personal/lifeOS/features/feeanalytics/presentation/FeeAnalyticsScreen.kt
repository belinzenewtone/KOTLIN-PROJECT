package com.personal.lifeOS.features.feeanalytics.presentation

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.personal.lifeOS.core.database.dao.CategoryTotal
import com.personal.lifeOS.core.database.entity.TransactionEntity
import com.personal.lifeOS.core.ui.designsystem.AppCard
import com.personal.lifeOS.core.ui.designsystem.EmptyState
import com.personal.lifeOS.core.ui.designsystem.InlineBanner
import com.personal.lifeOS.core.ui.designsystem.InlineBannerTone
import com.personal.lifeOS.core.ui.designsystem.LoadingState
import com.personal.lifeOS.core.ui.designsystem.PageScaffold
import com.personal.lifeOS.core.utils.DateUtils
import com.personal.lifeOS.ui.theme.AppSpacing
import com.personal.lifeOS.ui.theme.Warning

@Composable
fun FeeAnalyticsScreen(
    onBack: (() -> Unit)? = null,
    viewModel: FeeAnalyticsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    PageScaffold(
        headerEyebrow = "Finance",
        title = "Service Charges",
        subtitle = "Airtime, Fuliza, and subscription spending this month",
        onBack = onBack,
        contentPadding = PaddingValues(bottom = AppSpacing.BottomSafeWithFloatingNav),
    ) {
        state.error?.let {
            InlineBanner(message = it, tone = InlineBannerTone.ERROR)
        }

        if (state.isLoading) {
            LoadingState(label = "Calculating charges…")
            return@PageScaffold
        }

        if (state.categoryBreakdown.isEmpty()) {
            EmptyState(
                title = "No service charges this month",
                description = "No Airtime, Fuliza, or subscription transactions found for the current month.",
            )
            return@PageScaffold
        }

        // ── Total card ──────────────────────────────────────────────────────
        AppCard(modifier = Modifier.fillMaxWidth(), elevated = true) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "This Month's Charges",
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = DateUtils.formatCurrency(state.totalFeesThisMonth),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = Warning,
                )
            }
        }

        // ── Category breakdown bars ─────────────────────────────────────────
        Text(
            text = "By Category",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        AppCard(modifier = Modifier.fillMaxWidth(), elevated = true) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                state.categoryBreakdown.forEach { catTotal ->
                    FeeBarRow(
                        category = catTotal,
                        maxTotal = state.totalFeesThisMonth.coerceAtLeast(1.0),
                    )
                }
            }
        }

        // ── Recent transactions ─────────────────────────────────────────────
        if (state.recentFeeTransactions.isNotEmpty()) {
            Text(
                text = "Transactions",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                state.recentFeeTransactions.take(20).forEach { tx ->
                    FeeTxRow(transaction = tx)
                }
            }
        }
    }
}

@Composable
private fun FeeBarRow(category: CategoryTotal, maxTotal: Double) {
    val ratio = (category.total / maxTotal).toFloat().coerceIn(0f, 1f)
    val animatedRatio by animateFloatAsState(
        targetValue = ratio,
        animationSpec = tween(500),
        label = "fee_bar_${category.category}",
    )
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = category.category.lowercase().replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = DateUtils.formatCurrency(category.total),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = Warning,
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedRatio)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Warning),
            )
        }
    }
}

@Composable
private fun FeeTxRow(transaction: TransactionEntity) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = transaction.merchant,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "${transaction.category} · ${DateUtils.formatDate(transaction.date, "MMM d")}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = DateUtils.formatCurrency(transaction.amount),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
        )
    }
}
