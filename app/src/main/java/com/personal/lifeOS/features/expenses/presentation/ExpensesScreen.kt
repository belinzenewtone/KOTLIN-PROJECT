package com.personal.lifeOS.features.expenses.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.personal.lifeOS.ui.components.GlassCard
import com.personal.lifeOS.ui.theme.TextSecondary

@Composable
fun ExpensesScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Expenses",
            style = MaterialTheme.typography.headlineLarge
        )
        Text(
            text = "Track your MPESA transactions",
            style = MaterialTheme.typography.bodyLarge,
            color = TextSecondary
        )

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                Text("This Month", style = MaterialTheme.typography.titleMedium)
                Text("KES 0.00", style = MaterialTheme.typography.displayLarge)
            }
        }

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Text("Transaction list will render here", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        }
    }
}
