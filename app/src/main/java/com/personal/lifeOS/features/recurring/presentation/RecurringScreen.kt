package com.personal.lifeOS.features.recurring.presentation

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.personal.lifeOS.core.ui.designsystem.PageScaffold
import com.personal.lifeOS.ui.theme.AppSpacing

@Composable
fun RecurringScreen(
    viewModel: RecurringViewModel = hiltViewModel(),
    onBack: (() -> Unit)? = null,
) {
    val state by viewModel.uiState.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        PageScaffold(
            headerEyebrow = "Automation",
            title = "Recurring",
            subtitle = "Subscriptions and repeating items",
            contentPadding = PaddingValues(bottom = AppSpacing.BottomSafeWithFloatingNav),
            onBack = onBack,
        ) {
            if (state.rules.isEmpty()) {
                RecurringEmptyStateCard()
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(AppSpacing.Section),
                ) {
                    state.rules.forEach { rule ->
                        RecurringRuleCard(
                            rule = rule,
                            onToggle = { enabled -> viewModel.toggleEnabled(rule, enabled) },
                            onDelete = { viewModel.deleteRule(rule.id) },
                        )
                    }
                }
            }
        }

        // Clearly visible FAB for adding recurring rules
        ExtendedFloatingActionButton(
            onClick = viewModel::showAddDialog,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(
                    end = AppSpacing.ScreenHorizontal,
                    bottom = AppSpacing.BottomSafeWithFloatingNav + 8.dp,
                ),
            icon = {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "Add recurring rule",
                )
            },
            text = { Text("Add rule") },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            elevation = FloatingActionButtonDefaults.elevation(
                defaultElevation = 6.dp,
                pressedElevation = 10.dp,
            ),
        )
    }

    if (state.showDialog) {
        AddRecurringRuleDialog(
            state = state,
            onDismiss = viewModel::hideDialog,
            onSetTitle = viewModel::setTitle,
            onSetAmount = viewModel::setAmount,
            onSetType = viewModel::setType,
            onSetCadence = viewModel::setCadence,
            onSave = viewModel::saveRule,
        )
    }
}
