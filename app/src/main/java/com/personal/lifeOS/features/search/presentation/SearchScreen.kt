package com.personal.lifeOS.features.search.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.personal.lifeOS.core.ui.designsystem.AppCard
import com.personal.lifeOS.core.ui.designsystem.EmptyState
import com.personal.lifeOS.core.ui.designsystem.InlineBanner
import com.personal.lifeOS.core.ui.designsystem.InlineBannerTone
import com.personal.lifeOS.core.ui.designsystem.LoadingState
import com.personal.lifeOS.core.ui.designsystem.PageScaffold
import com.personal.lifeOS.core.ui.designsystem.SearchField
import com.personal.lifeOS.core.ui.designsystem.SegmentedControl
import com.personal.lifeOS.core.utils.DateUtils
import com.personal.lifeOS.features.search.domain.model.SearchResult
import com.personal.lifeOS.features.search.domain.model.SearchSource

@Composable
fun SearchScreen(viewModel: SearchViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()
    var selectedFilter by rememberSaveable { mutableStateOf(SearchResultFilter.ALL) }
    val filteredResults =
        remember(state.results, selectedFilter) {
            state.results.filterBy(selectedFilter)
        }

    PageScaffold(
        title = "Search",
        subtitle = "Cross-domain lookup for tasks, events, finance, and recurring rules.",
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SearchField(
                value = state.query,
                onValueChange = viewModel::setQuery,
                placeholder = "Search title, merchant, or category",
                modifier = Modifier.weight(1f),
            )
            OutlinedButton(
                onClick = viewModel::runSearch,
                enabled = !state.isLoading,
            ) {
                Text("Search")
            }
        }

        state.error?.let { message ->
            InlineBanner(
                message = message,
                tone = InlineBannerTone.ERROR,
            )
        }

        SegmentedControl(
            items = SearchResultFilter.entries.map { it.label },
            selectedIndex = SearchResultFilter.entries.indexOf(selectedFilter),
            onSelected = { selectedFilter = SearchResultFilter.entries[it] },
        )

        if (state.isLoading) {
            LoadingState(label = "Searching...")
            return@PageScaffold
        }

        if (state.results.isEmpty()) {
            EmptyState(
                title = "No search results yet",
                description =
                    "Run a search to scan transactions, tasks, events, budgets, " +
                        "incomes, and recurring rules.",
            )
            return@PageScaffold
        }

        if (filteredResults.isEmpty()) {
            EmptyState(
                title = "No results for this filter",
                description = "Try another filter or refine your query.",
            )
            return@PageScaffold
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            filteredResults.forEach { result ->
                SearchResultCard(result = result)
            }
        }
    }
}

@Composable
private fun SearchResultCard(result: SearchResult) {
    AppCard(modifier = Modifier.fillMaxWidth(), elevated = true) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(result.title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = result.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "${result.source.name} • ${DateUtils.formatDate(result.timestamp, "MMM dd, yyyy h:mm a")}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private enum class SearchResultFilter(
    val label: String,
) {
    ALL("All"),
    TASKS("Tasks"),
    EVENTS("Events"),
    FINANCE("Finance"),
    RECURRING("Recurring"),
}

private fun List<SearchResult>.filterBy(filter: SearchResultFilter): List<SearchResult> {
    return when (filter) {
        SearchResultFilter.ALL -> this
        SearchResultFilter.TASKS -> filter { it.source == SearchSource.TASK }
        SearchResultFilter.EVENTS -> filter { it.source == SearchSource.EVENT }
        SearchResultFilter.FINANCE ->
            filter {
                it.source == SearchSource.TRANSACTION ||
                    it.source == SearchSource.BUDGET ||
                    it.source == SearchSource.INCOME
            }
        SearchResultFilter.RECURRING -> filter { it.source == SearchSource.RECURRING_RULE }
    }
}
