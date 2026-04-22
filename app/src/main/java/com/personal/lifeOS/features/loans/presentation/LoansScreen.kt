package com.personal.lifeOS.features.loans.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.personal.lifeOS.core.database.entity.FulizaLoanEntity
import com.personal.lifeOS.core.ui.designsystem.AppCard
import com.personal.lifeOS.core.ui.designsystem.EmptyState
import com.personal.lifeOS.core.ui.designsystem.InlineBanner
import com.personal.lifeOS.core.ui.designsystem.InlineBannerTone
import com.personal.lifeOS.core.ui.designsystem.LoadingState
import com.personal.lifeOS.core.ui.designsystem.PageScaffold
import com.personal.lifeOS.core.utils.DateUtils
import com.personal.lifeOS.ui.theme.AppSpacing
import com.personal.lifeOS.ui.theme.Success
import com.personal.lifeOS.ui.theme.Warning

@Composable
fun LoansScreen(
    onBack: (() -> Unit)? = null,
    viewModel: LoansViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    PageScaffold(
        headerEyebrow = "Finance Tools",
        title = "Loans & Fuliza",
        subtitle = "Track outstanding draws and repayment history",
        onBack = onBack,
        contentPadding = PaddingValues(bottom = AppSpacing.BottomSafeWithFloatingNav),
    ) {
        state.error?.let {
            InlineBanner(message = it, tone = InlineBannerTone.ERROR)
        }

        if (state.isLoading) {
            LoadingState(label = "Loading loan history…")
            return@PageScaffold
        }

        // ── Outstanding summary card ────────────────────────────────────────
        AppCard(modifier = Modifier.fillMaxWidth(), elevated = true) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Net Outstanding",
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = DateUtils.formatCurrency(state.netOutstanding),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = if (state.netOutstanding > 0) Warning else Success,
                )
                if (state.netOutstanding <= 0.0) {
                    Text(
                        text = "All Fuliza draws are fully repaid.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Text(
                        text = "${state.openLoans.size} open draw${if (state.openLoans.size != 1) "s" else ""}. Pay to avoid daily interest.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        // ── Open / partially repaid loans ──────────────────────────────────
        if (state.openLoans.isNotEmpty()) {
            Text(
                text = "Open Draws",
                style = MaterialTheme.typography.titleSmall,
                color = Warning,
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                state.openLoans.forEach { loan ->
                    LoanCard(loan = loan)
                }
            }
        }

        // ── Closed loans ───────────────────────────────────────────────────
        if (state.closedLoans.isNotEmpty()) {
            HorizontalDivider()
            Text(
                text = "Repaid",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                state.closedLoans.take(10).forEach { loan ->
                    LoanCard(loan = loan)
                }
            }
        }

        if (state.openLoans.isEmpty() && state.closedLoans.isEmpty()) {
            EmptyState(
                title = "No Fuliza history yet",
                description = "Import M-Pesa messages from Finance to track Fuliza draws and repayments automatically.",
            )
        }
    }
}

@Composable
private fun LoanCard(loan: FulizaLoanEntity) {
    val outstanding = loan.outstandingKes
    val isClosed = loan.status == "CLOSED"

    AppCard(modifier = Modifier.fillMaxWidth(), elevated = true) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = "Draw: ${DateUtils.formatCurrency(loan.drawAmountKes)}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = DateUtils.formatDate(loan.drawDate, "MMM dd, yyyy"),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = if (isClosed) "Repaid" else DateUtils.formatCurrency(outstanding),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isClosed) Success else Warning,
                    )
                    Text(
                        text = loan.status.lowercase().replaceFirstChar { it.uppercase() }.replace("_", " "),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (loan.totalRepaidKes > 0.0) {
                Text(
                    text = "Repaid: ${DateUtils.formatCurrency(loan.totalRepaidKes)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
