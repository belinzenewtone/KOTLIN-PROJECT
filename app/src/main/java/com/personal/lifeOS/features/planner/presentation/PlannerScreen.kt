package com.personal.lifeOS.features.planner.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.personal.lifeOS.feature.budget.presentation.BudgetScreen
import com.personal.lifeOS.feature.export.presentation.ExportScreen
import com.personal.lifeOS.feature.income.presentation.IncomeScreen
import com.personal.lifeOS.feature.recurring.presentation.RecurringScreen
import com.personal.lifeOS.feature.search.presentation.SearchScreen
import com.personal.lifeOS.feature.settings.presentation.SettingsScreen
import com.personal.lifeOS.ui.theme.AppSpacing
import com.personal.lifeOS.ui.theme.BackgroundDark

@Composable
fun PlannerScreen() {
    val tabs = listOf("Budget", "Income", "Recurring", "Search", "Export", "Settings")
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(BackgroundDark)
                .statusBarsPadding()
                .padding(bottom = AppSpacing.BottomSafe),
    ) {
        Text(
            text = "Planner",
            style = MaterialTheme.typography.headlineSmall,
            modifier =
                Modifier.padding(
                    horizontal = AppSpacing.ScreenHorizontal,
                    vertical = AppSpacing.Section,
                ),
        )

        ScrollableTabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { index, label ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(label) },
                )
            }
        }

        when (selectedTab) {
            0 -> BudgetScreen()
            1 -> IncomeScreen()
            2 -> RecurringScreen()
            3 -> SearchScreen()
            4 -> ExportScreen()
            else -> SettingsScreen()
        }
    }
}
