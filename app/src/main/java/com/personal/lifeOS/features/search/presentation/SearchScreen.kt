package com.personal.lifeOS.features.search.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
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
import com.personal.lifeOS.core.utils.DateUtils
import com.personal.lifeOS.features.search.domain.model.SearchResult
import com.personal.lifeOS.features.search.domain.model.SearchSource
import com.personal.lifeOS.ui.theme.AppSpacing

/** Maximum results shown per group before a "Show X more" button appears. */
private const val RESULTS_PER_GROUP = 5

@Composable
fun SearchScreen(
    viewModel: SearchViewModel = hiltViewModel(),
    onBack: (() -> Unit)? = null,
    onOpenResult: (SearchResult) -> Unit = {},
) {
    val state by viewModel.uiState.collectAsState()
    var selectedFilter by rememberSaveable { mutableStateOf(SearchResultFilter.ALL) }
    // Per-group expand state — tracks which group labels are fully expanded
    var expandedGroups by remember { mutableStateOf(emptySet<String>()) }

    val filteredResults =
        remember(state.results, selectedFilter) { state.results.filterBy(selectedFilter) }
    val groupedResults =
        remember(filteredResults) { filteredResults.groupBy { it.source.groupLabel } }

    PageScaffold(
        headerEyebrow = "Global Lookup",
        title = "Search",
        subtitle = "Search across tasks, events, and finance",
        onBack = onBack,
        contentPadding = PaddingValues(bottom = AppSpacing.BottomSafeWithFloatingNav),
    ) {
        AppCard(modifier = Modifier.fillMaxWidth(), elevated = false) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SearchField(
                    value = state.query,
                    onValueChange = {
                        viewModel.setQuery(it)
                        // Reset group expansion whenever the query changes
                        expandedGroups = emptySet()
                    },
                    placeholder = "Search names, merchants, categories…",
                    modifier = Modifier.fillMaxWidth(),
                )
                if (state.query.isNotBlank()) {
                    TextButton(onClick = { viewModel.setQuery(""); expandedGroups = emptySet() }) {
                        Text("Clear")
                    }
                }
                Text(
                    text = "Searches tasks, events, transactions, budgets, incomes, and recurring rules.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        state.error?.let { message ->
            InlineBanner(message = message, tone = InlineBannerTone.ERROR)
        }

        // ── Recent searches — shown when query is blank ────────────────────
        if (state.query.isBlank() && state.recentSearches.isNotEmpty()) {
            RecentSearchesRow(
                searches = state.recentSearches,
                onSearchClick = { viewModel.setQuery(it) },
                onClear = { viewModel.clearRecentSearches() },
            )
        }

        // ── Filter chips ───────────────────────────────────────────────────
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 0.dp, vertical = 4.dp),
        ) {
            items(SearchResultFilter.entries.size) { idx ->
                val filter = SearchResultFilter.entries[idx]
                val selected = filter == selectedFilter
                Text(
                    text = filter.label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    color =
                        if (selected) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier =
                        Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                if (selected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant,
                            )
                            .clickable { selectedFilter = filter; expandedGroups = emptySet() }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                )
            }
        }

        SearchResultsContent(
            state = state,
            filteredResults = filteredResults,
            groupedResults = groupedResults,
            expandedGroups = expandedGroups,
            onToggleGroup = { label ->
                expandedGroups =
                    if (label in expandedGroups) expandedGroups - label else expandedGroups + label
            },
            onOpenResult = onOpenResult,
        )
    }
}

// ── Results content ────────────────────────────────────────────────────────────

@Composable
private fun SearchResultsContent(
    state: SearchUiState,
    filteredResults: List<SearchResult>,
    groupedResults: Map<String, List<SearchResult>>,
    expandedGroups: Set<String>,
    onToggleGroup: (String) -> Unit,
    onOpenResult: (SearchResult) -> Unit,
) {
    when {
        state.isLoading -> LoadingState(label = "Searching…")
        state.query.isBlank() -> Unit // idle — recent searches handle this state
        state.results.isEmpty() -> SearchEmptyState()
        filteredResults.isEmpty() ->
            EmptyState(
                title = "No results for this filter",
                description = "Try another filter or refine your query.",
            )
        else -> {
            SearchResultCount(count = filteredResults.size)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (state.groupedSearchEnabled) {
                    groupedResults.forEach { (group, results) ->
                        SearchResultGroup(
                            title = group,
                            results = results,
                            expanded = group in expandedGroups,
                            onToggleExpand = { onToggleGroup(group) },
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

// ── Recent searches ────────────────────────────────────────────────────────────

@Composable
private fun RecentSearchesRow(
    searches: List<String>,
    onSearchClick: (String) -> Unit,
    onClear: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Recent",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(onClick = onClear) {
                Text("Clear", style = MaterialTheme.typography.labelSmall)
            }
        }
        // Show newest first
        searches.reversed().forEach { query ->
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onSearchClick(query) }
                        .padding(horizontal = 4.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.History,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = query,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

// ── Group with show-more ───────────────────────────────────────────────────────

@Composable
private fun SearchResultGroup(
    title: String,
    results: List<SearchResult>,
    expanded: Boolean,
    onToggleExpand: () -> Unit,
    onOpenResult: (SearchResult) -> Unit,
) {
    val visibleResults = if (expanded) results else results.take(RESULTS_PER_GROUP)
    val hiddenCount = results.size - RESULTS_PER_GROUP

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        visibleResults.forEach { result ->
            SearchResultCard(result = result, onOpenResult = onOpenResult)
        }
        if (hiddenCount > 0 && !expanded) {
            TextButton(onClick = onToggleExpand) {
                Text(
                    "Show $hiddenCount more in $title",
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        } else if (expanded && results.size > RESULTS_PER_GROUP) {
            TextButton(onClick = onToggleExpand) {
                Text("Show less", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

// ── Shared helpers ─────────────────────────────────────────────────────────────

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
                    "${result.source.groupLabel} • ${DateUtils.formatDate(result.timestamp, "MMM dd, yyyy")}",
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

// ── Filter enum ────────────────────────────────────────────────────────────────

private enum class SearchResultFilter(val label: String) {
    ALL("All"),
    TASKS("Tasks"),
    EVENTS("Events"),
    BIRTHDAY("Birthdays"),
    ANNIVERSARY("Anniversaries"),
    COUNTDOWN("Countdowns"),
    FINANCE("Finance"),
    RECURRING("Recurring"),
}

private fun List<SearchResult>.filterBy(filter: SearchResultFilter): List<SearchResult> =
    when (filter) {
        SearchResultFilter.ALL -> this
        SearchResultFilter.TASKS -> filter { it.source == SearchSource.TASK }
        SearchResultFilter.EVENTS -> filter { it.source == SearchSource.EVENT }
        SearchResultFilter.BIRTHDAY -> filter { it.source == SearchSource.BIRTHDAY }
        SearchResultFilter.ANNIVERSARY -> filter { it.source == SearchSource.ANNIVERSARY }
        SearchResultFilter.COUNTDOWN -> filter { it.source == SearchSource.COUNTDOWN }
        SearchResultFilter.FINANCE ->
            filter {
                it.source == SearchSource.TRANSACTION ||
                    it.source == SearchSource.BUDGET ||
                    it.source == SearchSource.INCOME
            }
        SearchResultFilter.RECURRING -> filter { it.source == SearchSource.RECURRING_RULE }
    }
