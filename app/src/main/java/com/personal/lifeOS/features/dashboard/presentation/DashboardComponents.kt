package com.personal.lifeOS.features.dashboard.presentation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.personal.lifeOS.R
import com.personal.lifeOS.core.ui.designsystem.AppDesignTokens
import com.personal.lifeOS.core.utils.DateUtils
import com.personal.lifeOS.features.dashboard.domain.model.DailySpending
import com.personal.lifeOS.features.dashboard.domain.model.DashboardInsight
import com.personal.lifeOS.features.dashboard.domain.model.RecentTransaction
import com.personal.lifeOS.features.dashboard.domain.model.UpcomingEvent
import com.personal.lifeOS.ui.components.AccentGlassCard
import com.personal.lifeOS.ui.components.GlassCard
import com.personal.lifeOS.ui.theme.Accent
import com.personal.lifeOS.ui.theme.Info
import com.personal.lifeOS.ui.theme.Success
import com.personal.lifeOS.ui.theme.Warning

@Composable
internal fun DashboardHeader(greeting: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Image(
            painter = painterResource(id = R.drawable.logo_personalos_mark),
            contentDescription = "PersonalOS logo",
            modifier =
                Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(AppDesignTokens.radius.sm)),
            contentScale = ContentScale.Crop,
        )
        Spacer(Modifier.width(12.dp))
        Column {
            Text(greeting, style = MaterialTheme.typography.headlineLarge)
            Text(
                "Here's your day at a glance",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
internal fun DashboardSpendingSummary(
    today: Double,
    week: Double,
    month: Double,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        DashboardSummaryCard(title = "Today", amount = today, accented = true)
        DashboardSummaryCard(title = "This Week", amount = week, accented = false)
        DashboardSummaryCard(title = "This Month", amount = month, accented = false)
    }
}

@Composable
private fun DashboardSummaryCard(
    title: String,
    amount: Double,
    accented: Boolean,
) {
    val cardModifier = Modifier.width(160.dp)
    if (accented) {
        AccentGlassCard(modifier = cardModifier) {
            Column {
                Text(title, style = MaterialTheme.typography.labelSmall)
                Text(
                    text = DateUtils.formatCurrency(amount),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    } else {
        GlassCard(modifier = cardModifier) {
            Column {
                Text(title, style = MaterialTheme.typography.labelSmall)
                Text(
                    text = DateUtils.formatCurrency(amount),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
internal fun ProductivityCard(
    completedTodayCount: Int,
    pendingTaskCount: Int,
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier =
                    Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.CheckCircle, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text("Productivity", style = MaterialTheme.typography.titleMedium)
                Text(
                    "$completedTodayCount completed today · $pendingTaskCount pending",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
internal fun UpcomingEventsCard(events: List<UpcomingEvent>) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.CalendarMonth, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Upcoming Events", style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(12.dp))
            if (events.isEmpty()) {
                Text(
                    "No upcoming events",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                events.forEach { event ->
                    EventRow(event = event)
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun EventRow(event: UpcomingEvent) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(
                        when (event.type) {
                            "WORK" -> Info
                            "HEALTH" -> Success
                            "FINANCE" -> Warning
                            else -> MaterialTheme.colorScheme.primary
                        },
                    ),
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(event.title, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
            Text(
                DateUtils.formatDate(event.date, "EEE, MMM dd · h:mm a"),
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

@Composable
internal fun WeeklySpendingCard(data: List<DailySpending>) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.AutoMirrored.Filled.TrendingUp, null, tint = Accent, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Weekly Spending", style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(16.dp))
            WeeklyBarChart(data = data)
        }
    }
}

@Composable
private fun WeeklyBarChart(data: List<DailySpending>) {
    if (data.isEmpty()) {
        Text(
            "No spending data this week",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }

    val maxAmount = data.maxOfOrNull { it.amount } ?: 1.0
    val barMaxHeight = 100.dp

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom,
    ) {
        data.forEach { day ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
            ) {
                if (day.amount > 0) {
                    Text(
                        DateUtils.formatCurrency(day.amount).replace("KES ", ""),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(4.dp))
                }

                val fraction = if (maxAmount > 0) (day.amount / maxAmount).toFloat() else 0f
                val barHeight = (barMaxHeight.value * fraction.coerceAtLeast(0.02f)).dp

                Box(
                    modifier =
                        Modifier
                            .width(28.dp)
                            .height(barHeight)
                            .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                            .background(
                                if (day.amount > 0) {
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.7f + fraction * 0.3f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                },
                            ),
                )

                Spacer(Modifier.height(4.dp))
                Text(day.dayLabel, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
internal fun DashboardInsightsCard(insights: List<DashboardInsight>) {
    if (insights.isEmpty()) return

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.AutoAwesome, null, tint = Accent, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Insights", style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(12.dp))

            insights.forEachIndexed { index, insight ->
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(insight.title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(Modifier.height(4.dp))
                    Text(insight.body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (insight.isAiGenerated) {
                        Spacer(Modifier.height(4.dp))
                        Text("AI generated", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                if (index < insights.lastIndex) {
                    Spacer(Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
internal fun RecentTransactionsCard(transactions: List<RecentTransaction>) {
    if (transactions.isEmpty()) return

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Text("Recent Transactions", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))
            transactions.forEach { tx ->
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(tx.merchant, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                        Text(tx.category, style = MaterialTheme.typography.labelSmall)
                    }
                    Text(
                        text = DateUtils.formatCurrency(tx.amount),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}
