package com.personal.lifeOS.feature.search.presentation

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import com.personal.lifeOS.features.search.domain.model.SearchResult
import com.personal.lifeOS.features.search.presentation.SearchScreen as LegacySearchScreen
import com.personal.lifeOS.features.search.presentation.SearchViewModel

@Composable
fun SearchScreen(
    viewModel: SearchViewModel = hiltViewModel(),
    onOpenResult: (SearchResult) -> Unit = {},
) {
    LegacySearchScreen(
        viewModel = viewModel,
        onOpenResult = onOpenResult,
    )
}
