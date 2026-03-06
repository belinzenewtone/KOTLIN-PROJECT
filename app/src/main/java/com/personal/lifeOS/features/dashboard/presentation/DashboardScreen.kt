package com.personal.lifeOS.features.dashboard.presentation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.personal.lifeOS.R
import androidx.hilt.navigation.compose.hiltViewModel
import com.personal.lifeOS.core.utils.DateUtils
import com.personal.lifeOS.features.dashboard.domain.model.DailySpending
import com.personal.lifeOS.features.dashboard.domain.model.UpcomingEvent
import com.personal.lifeOS.ui.components.AccentGlassCard
import com.personal.lifeOS.ui.components.GlassCard
import com.personal.lifeOS.ui.theme.*

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val data = state.data

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(top = 8.dp, bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header with logo
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Image(
                painter = painterResource(id = R.drawable.logo),
                contentDescription = "BELTECH Logo",
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(Modifier.width(12.dp))
            Column {
                Text(data.greeting, style = MaterialTheme.typography.headlineLarge)
                Text(
                    "Here's your day at a glance",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
        }

        if (state.isLoading) {
            Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Primary)
            }
        } else {
            // Spending summary row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AccentGlassCard(modifier = Modifier.width(160.dp)) {
                    Column {
                        Text("Today", style = MaterialTheme.typography.labelSmall)
                        Text(
                            DateUtils.formatCurrency(data.todaySpending),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                GlassCard(modifier = Modifier.width(160.dp)) {
                    Column {
                        Text("This Week", style = MaterialTheme.typography.labelSmall)
                        Text(
                            DateUtils.formatCurrency(data.weekSpending),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                GlassCard(modifier = Modifier.width(160.dp)) {
                    Column {
                        Text("This Month", style = MaterialTheme.typography.labelSmall)
                        Text(
                            DateUtils.formatCurrency(data.monthSpending),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Productivity card
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Primary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.CheckCircle, null, tint = Primary, modifier = Modifier.size(24.dp))
                    }
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text("Productivity", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "${data.completedTodayCount} completed today · ${data.pendingTaskCount} pending",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }
            }

            // Upcoming events
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.CalendarMonth, null, tint = Primary, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Upcoming Events", style = MaterialTheme.typography.titleMedium)
                    }
                    Spacer(Modifier.height(12.dp))
                    if (data.upcomingEvents.isEmpty()) {
                        Text(
                            "No upcoming events",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    } else {
                        data.upcomingEvents.forEach { event ->
                            EventRow(event)
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                }
            }

            // Weekly spending chart
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.TrendingUp, null, tint = Accent, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Weekly Spending", style = MaterialTheme.typography.titleMedium)
                    }
                    Spacer(Modifier.height(16.dp))
                    WeeklyBarChart(data.weeklySpendingData)
                }
            }

            // Recent transactions
            if (data.recentTransactions.isNotEmpty()) {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        Text("Recent Transactions", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(12.dp))
                        data.recentTransactions.forEach { tx ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(tx.merchant, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                                    Text(tx.category, style = MaterialTheme.typography.labelSmall)
                                }
                                Text(
                                    DateUtils.formatCurrency(tx.amount),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EventRow(event: UpcomingEvent) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(
                    when (event.type) {
                        "WORK" -> Info
                        "HEALTH" -> Success
                        "FINANCE" -> Warning
                        else -> Primary
                    }
                )
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(event.title, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
            Text(
                DateUtils.formatDate(event.date, "EEE, MMM dd · h:mm a"),
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

@Composable
private fun WeeklyBarChart(data: List<DailySpending>) {
    if (data.isEmpty()) {
        Text("No spending data this week", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        return
    }

    val maxAmount = data.maxOfOrNull { it.amount } ?: 1.0
    val barMaxHeight = 100.dp

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom
    ) {
        data.forEach { day ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom
            ) {
                if (day.amount > 0) {
                    Text(
                        DateUtils.formatCurrency(day.amount).replace("KES ", ""),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextTertiary
                    )
                    Spacer(Modifier.height(4.dp))
                }

                val fraction = if (maxAmount > 0) (day.amount / maxAmount).toFloat() else 0f
                val barHeight = (barMaxHeight.value * fraction.coerceAtLeast(0.02f)).dp

                Box(
                    modifier = Modifier
                        .width(28.dp)
                        .height(barHeight)
                        .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                        .background(
                            if (day.amount > 0) Primary.copy(alpha = 0.7f + fraction * 0.3f)
                            else GlassWhite
                        )
                )

                Spacer(Modifier.height(4.dp))
                Text(day.dayLabel, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}
