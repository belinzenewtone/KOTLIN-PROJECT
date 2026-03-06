package com.personal.lifeOS.features.dashboard.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.personal.lifeOS.ui.components.GlassCard
import com.personal.lifeOS.ui.theme.TextSecondary

@Composable
fun DashboardScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Good Morning",
            style = MaterialTheme.typography.headlineLarge
        )
        Text(
            text = "Here's your day at a glance",
            style = MaterialTheme.typography.bodyLarge,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Daily Expense Card
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                Text("Today's Spending", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Text("KES 0.00", style = MaterialTheme.typography.displayLarge)
            }
        }

        // Upcoming Events Card
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                Text("Upcoming Events", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Text("No upcoming events", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            }
        }

        // Productivity Card
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                Text("Productivity", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Text("0 tasks completed today", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            }
        }

        // Weekly Spending Chart placeholder
        GlassCard(modifier = Modifier.fillMaxWidth().height(200.dp)) {
            Column {
                Text("Weekly Spending", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Chart will render here", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            }
        }
    }
}
