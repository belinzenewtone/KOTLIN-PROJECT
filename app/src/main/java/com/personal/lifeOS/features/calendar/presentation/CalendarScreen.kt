package com.personal.lifeOS.features.calendar.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.personal.lifeOS.ui.theme.AppSpacing
import com.personal.lifeOS.ui.theme.BackgroundDark
import com.personal.lifeOS.ui.theme.Primary
import com.personal.lifeOS.ui.theme.TextPrimary

@Composable
fun CalendarScreen(viewModel: CalendarViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        containerColor = BackgroundDark,
        floatingActionButton = {
            FloatingActionButton(
                modifier =
                    Modifier
                        .navigationBarsPadding()
                        .padding(bottom = AppSpacing.FabBottomOffset),
                onClick = { viewModel.showAddDialog() },
                containerColor = Primary,
                contentColor = TextPrimary,
                shape = CircleShape,
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add event")
            }
        },
    ) { padding ->
        LazyColumn(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .statusBarsPadding()
                    .padding(horizontal = AppSpacing.ScreenHorizontal),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.Section),
            contentPadding = PaddingValues(bottom = AppSpacing.BottomSafeWithFab),
        ) {
            item {
                Spacer(Modifier.height(AppSpacing.ScreenTop))
                Text("Calendar", style = MaterialTheme.typography.headlineLarge)
                Spacer(Modifier.height(AppSpacing.ScreenTop))
            }

            item {
                CalendarMonthCard(
                    state = state,
                    onPreviousMonth = { viewModel.navigateMonth(-1) },
                    onNextMonth = { viewModel.navigateMonth(1) },
                    onDateSelected = { viewModel.selectDate(it) },
                )
            }

            item {
                SelectedDateHeader(selectedDate = state.selectedDate)
            }

            if (state.selectedDayEvents.isEmpty()) {
                item { EmptyDayEventsCard() }
            }

            items(state.selectedDayEvents, key = { it.id }) { event ->
                CalendarEventCard(
                    event = event,
                    onComplete = { viewModel.markEventCompleted(event) },
                    onEdit = { viewModel.showEditDialog(event) },
                    onDelete = { viewModel.deleteEvent(event) },
                )
            }
        }
    }

    if (state.showAddDialog) {
        AddEventDialog(
            initialEvent = state.editingEvent,
            selectedDate = state.selectedDate,
            onDismiss = { viewModel.hideAddDialog() },
            onSave = { title, desc, type, importance, eventDate, endDate ->
                viewModel.saveEvent(title, desc, type, importance, eventDate, endDate)
            },
        )
    }
}
