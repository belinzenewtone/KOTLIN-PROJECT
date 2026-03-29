package com.personal.lifeOS.features.learning.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.personal.lifeOS.core.ui.designsystem.AppDesignTokens
import com.personal.lifeOS.core.ui.designsystem.EmptyState
import com.personal.lifeOS.core.ui.designsystem.PageScaffold
import com.personal.lifeOS.features.learning.domain.model.LearningCategory
import com.personal.lifeOS.features.learning.domain.model.LearningSession
import com.personal.lifeOS.ui.theme.AppSpacing
import com.personal.lifeOS.ui.theme.Success

@Composable
fun LearningScreen(
    viewModel: LearningViewModel = hiltViewModel(),
    onBack: (() -> Unit)? = null,
) {
    val state by viewModel.uiState.collectAsState()

    val filtered = state.sessions.let { sessions ->
        val cat = state.selectedCategory
        if (cat == null) sessions else sessions.filter { it.category == cat }
    }

    val completedCount = state.sessions.count { it.isCompleted }

    PageScaffold(
        headerEyebrow = "Growth",
        title = "Learn",
        subtitle = "$completedCount of ${state.sessions.size} sessions completed",
        onBack = onBack,
        contentPadding = PaddingValues(bottom = AppSpacing.BottomSafeWithFloatingNav),
    ) {
        // Category filter chips
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                FilterChip(
                    selected = state.selectedCategory == null,
                    onClick = { viewModel.selectCategory(null) },
                    label = { Text("All") },
                )
            }
            items(LearningCategory.entries) { cat ->
                FilterChip(
                    selected = state.selectedCategory == cat,
                    onClick = { viewModel.selectCategory(cat) },
                    label = { Text(cat.label) },
                )
            }
        }

        if (filtered.isEmpty()) {
            EmptyState(
                title = "No sessions here",
                description = "Select a different category to see more learning content.",
            )
        } else {
            filtered.forEach { session ->
                LearningSessionCard(
                    session = session,
                    onTap = {
                        if (!session.isCompleted) {
                            viewModel.markCompleted(session.id)
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun LearningSessionCard(
    session: LearningSession,
    onTap: () -> Unit,
) {
    val shape = RoundedCornerShape(AppDesignTokens.radius.md)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface, shape)
            .clickable(onClick = onTap)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = session.category.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = session.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            if (session.isCompleted) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = "Completed",
                    tint = Success,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }

        Text(
            text = session.description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.Timer,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .padding(end = 2.dp)
                    .height(14.dp),
            )
            Text(
                text = "${session.durationMinutes} min",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (session.progress > 0f && !session.isCompleted) {
            LinearProgressIndicator(
                progress = { session.progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
        }

        if (!session.isCompleted) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(AppDesignTokens.radius.sm))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                    .padding(horizontal = 12.dp, vertical = 4.dp),
            ) {
                Text(
                    text = if (session.progress > 0f) "Continue" else "Start",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}
