package com.personal.lifeOS.features.planner.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Loop
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Search
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
    onOpenExport: () -> Unit = {},
    onOpenSearch: () -> Unit = {},
) {
    PageScaffold(
        headerEyebrow = "Finance Tools",
        title = "Finance Hub",
        subtitle = "Manage budgets, income, recurring items, and exports",
        onBack = onBack,
        contentPadding = PaddingValues(bottom = AppSpacing.BottomSafeWithFloatingNav),
    ) {
        ToolCard(
            icon = Icons.Filled.AccountBalance,
            title = "Budgets",
            description = "Set spending limits by category and track progress",
            onClick = onOpenBudget,
        )
        ToolCard(
            icon = Icons.Filled.MonetizationOn,
            title = "Income",
            description = "Log and review income sources",
            onClick = onOpenIncome,
        )
        ToolCard(
            icon = Icons.Filled.Loop,
            title = "Recurring",
            description = "Subscriptions, salaries, and scheduled payments",
            onClick = onOpenRecurring,
        )
        ToolCard(
            icon = Icons.Filled.Search,
            title = "Search Finance",
            description = "Search transactions, budgets, and recurring entries",
            onClick = onOpenSearch,
        )
        ToolCard(
            icon = Icons.Filled.FileDownload,
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
