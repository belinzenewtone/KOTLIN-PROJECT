package com.personal.lifeOS.features.search.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
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
import com.personal.lifeOS.ui.theme.AppSpacing
import com.personal.lifeOS.core.utils.DateUtils
import com.personal.lifeOS.features.search.domain.model.SearchResult
import com.personal.lifeOS.features.search.domain.model.SearchSource

@Composable
fun SearchScreen(
    viewModel: SearchViewModel = hiltViewModel(),
    onOpenResult: (SearchResult) -> Unit = {},
) {
    val state by viewModel.uiState.collectAsState()
    var selectedFilter by rememberSaveable { mutableStateOf(SearchResultFilter.ALL) }
    val filteredResults =
        remember(state.results, selectedFilter) {
            state.results.filterBy(selectedFilter)
        }
    val groupedResults =
        remember(filteredResults) {
            filteredResults.groupBy { it.source.groupLabel }
        }

    PageScaffold(
        title = "Search",
        subtitle = "Cross-domain lookup for tasks, finance, calendar, and recurring rules.",
        contentPadding = PaddingValues(bottom = AppSpacing.BottomSafeWithFloatingNav),
    ) {
        AppCard(modifier = Modifier.fillMaxWidth(), elevated = false) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SearchField(
                    value = state.query,
                    onValueChange = viewModel::setQuery,
                    placeholder = "Search title, merchant, or category",
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(
                        onClick = viewModel::runSearch,
                        enabled = !state.isLoading,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(if (state.isLoading) "Searching..." else "Search")
                    }
                    TextButton(
                        onClick = { viewModel.setQuery("") },
                        enabled = state.query.isNotBlank(),
                    ) {
                        Text("Clear")
                    }
                }
                Text(
                    text = "Search runs across tasks, events, transactions, budgets, incomes, and recurring rules.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
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

        SearchResultsContent(
            state = state,
            filteredResults = filteredResults,
            groupedResults = groupedResults,
            onOpenResult = onOpenResult,
        )
    }
}

@Composable
private fun SearchResultsContent(
    state: SearchUiState,
    filteredResults: List<SearchResult>,
    groupedResults: Map<String, List<SearchResult>>,
    onOpenResult: (SearchResult) -> Unit,
) {
    when {
        state.isLoading -> LoadingState(label = "Searching...")
        state.results.isEmpty() -> SearchEmptyState()
        filteredResults.isEmpty() -> {
            EmptyState(
                title = "No results for this filter",
                description = "Try another filter or refine your query.",
            )
        }
        else -> {
            SearchResultCount(count = filteredResults.size)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (state.groupedSearchEnabled) {
                    groupedResults.forEach { (group, results) ->
                        SearchResultGroup(
                            title = group,
                            results = results,
                            onOpenResult = onOpenResult,
                        )
                    }
                } else {
                    filteredResults.forEach { result ->
                        SearchResultCard(result = result, onOpenResult = onOpenResult)
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchEmptyState() {
    EmptyState(
        title = "No search results yet",
        description =
            "Run a search to scan transactions, tasks, events, budgets, " +
                "incomes, and recurring rules.",
    )
}

@Composable
private fun SearchResultCount(count: Int) {
    Text(
        text = "$count result${if (count == 1) "" else "s"}",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun SearchResultGroup(
    title: String,
    results: List<SearchResult>,
    onOpenResult: (SearchResult) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        results.forEach { result ->
            SearchResultCard(result = result, onOpenResult = onOpenResult)
        }
    }
}

@Composable
private fun SearchResultCard(
    result: SearchResult,
    onOpenResult: (SearchResult) -> Unit,
) {
    AppCard(modifier = Modifier.fillMaxWidth(), elevated = true) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(result.title, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = result.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "${result.source.groupLabel} • ${DateUtils.formatDate(result.timestamp, "MMM dd, yyyy h:mm a")}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (result.navigationTarget != null) {
                TextButton(onClick = { onOpenResult(result) }) {
                    Text("Open")
                }
            }
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
