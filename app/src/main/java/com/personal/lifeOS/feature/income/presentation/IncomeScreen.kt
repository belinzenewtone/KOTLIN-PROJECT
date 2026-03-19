package com.personal.lifeOS.feature.income.presentation

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import com.personal.lifeOS.features.income.presentation.IncomeScreen as LegacyIncomeScreen
import com.personal.lifeOS.features.income.presentation.IncomeViewModel

/**
 * Feature-package bridge retained during phased package migration.
 */
@Composable
fun IncomeScreen(viewModel: IncomeViewModel = hiltViewModel()) {
    LegacyIncomeScreen(viewModel = viewModel)
}
