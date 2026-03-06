package com.personal.lifeOS.features.tasks.presentation

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
fun TasksScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Tasks",
            style = MaterialTheme.typography.headlineLarge
        )

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                Text("Pending Tasks", style = MaterialTheme.typography.titleMedium)
                Text("0 tasks", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            }
        }
    }
}
