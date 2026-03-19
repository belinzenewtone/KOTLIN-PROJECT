package com.personal.lifeOS.feature.search.presentation

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import com.personal.lifeOS.features.search.presentation.SearchScreen as LegacySearchScreen
import com.personal.lifeOS.features.search.presentation.SearchViewModel

@Composable
fun SearchScreen(viewModel: SearchViewModel = hiltViewModel()) {
    LegacySearchScreen(viewModel = viewModel)
}
