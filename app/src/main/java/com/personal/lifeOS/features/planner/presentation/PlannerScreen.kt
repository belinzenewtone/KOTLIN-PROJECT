package com.personal.lifeOS.features.planner.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.Loop
import androidx.compose.material.icons.outlined.MonetizationOn
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.personal.lifeOS.core.ui.designsystem.AppCard
import com.personal.lifeOS.core.ui.designsystem.AppDesignTokens
import com.personal.lifeOS.core.ui.designsystem.PageScaffold
import com.personal.lifeOS.ui.theme.AppSpacing

/**
 * Finance Tools hub — replaces the old ScrollableTabRow god-screen.
 * Each tool is a tappable card that navigates to its own dedicated screen.
 */
@Composable
fun PlannerScreen(
    onBack: (() -> Unit)? = null,
    onOpenBudget: () -> Unit = {},
    onOpenIncome: () -> Unit = {},
    onOpenRecurring: () -> Unit = {},
    onOpenLoans: () -> Unit = {},
    onOpenExport: () -> Unit = {},
    onOpenSearch: () -> Unit = {},
) {
    PageScaffold(
        headerEyebrow = "Finance Tools",
        title = "Finance Hub",
        subtitle = "Manage budgets, income, recurring items, loans, and exports",
        onBack = onBack,
        contentPadding = PaddingValues(bottom = AppSpacing.BottomSafeWithFloatingNav),
    ) {
        ToolCard(
            icon = Icons.Outlined.AccountBalance,
            title = "Budgets",
            description = "Set spending limits by category and track progress",
            onClick = onOpenBudget,
        )
        ToolCard(
            icon = Icons.Outlined.MonetizationOn,
            title = "Income",
            description = "Log and review income sources",
            onClick = onOpenIncome,
        )
        ToolCard(
            icon = Icons.Outlined.Loop,
            title = "Recurring",
            description = "Subscriptions, salaries, and scheduled payments",
            onClick = onOpenRecurring,
        )
        ToolCard(
            icon = Icons.Outlined.AccountBalanceWallet,
            title = "Loans & Fuliza",
            description = "Track outstanding Fuliza draws and repayment history",
            onClick = onOpenLoans,
        )
        ToolCard(
            icon = Icons.Outlined.Search,
            title = "Search Finance",
            description = "Search transactions, budgets, and recurring entries",
            onClick = onOpenSearch,
        )
        ToolCard(
            icon = Icons.Outlined.FileDownload,
            title = "Export",
            description = "Export your data as CSV or share a report",
            onClick = onOpenExport,
        )
    }
}

@Composable
private fun ToolCard(
    icon: ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit,
) {
    AppCard(
        modifier = Modifier.clickable(onClick = onClick),
        elevated = true,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppDesignTokens.spacing.md),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(26.dp),
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
