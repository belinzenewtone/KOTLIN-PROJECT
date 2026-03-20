package com.personal.lifeOS.features.planner.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.personal.lifeOS.core.ui.designsystem.PageScaffold

/**
 * Finance Tools hub — replaces the old ScrollableTabRow god-screen.
 * Each tool is a tappable card that navigates to its own dedicated screen.
 */
@Composable
fun PlannerScreen(
    onOpenBudget: () -> Unit = {},
    onOpenIncome: () -> Unit = {},
    onOpenRecurring: () -> Unit = {},
    onOpenExport: () -> Unit = {},
    onOpenSearch: () -> Unit = {},
) {
    PageScaffold(
        title = "Finance Tools",
        subtitle = "Manage budgets, income, and recurring payments",
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
            title = "Search Transactions",
            description = "Full-text search across all ledger entries",
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
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface, shape)
            .clickable(onClick = onClick)
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(28.dp),
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
