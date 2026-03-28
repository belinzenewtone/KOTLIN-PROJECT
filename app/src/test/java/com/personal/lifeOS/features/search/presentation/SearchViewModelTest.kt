package com.personal.lifeOS.features.search.presentation

import com.personal.lifeOS.features.search.domain.model.SearchResult
import com.personal.lifeOS.features.search.domain.model.SearchSource
import com.personal.lifeOS.features.search.domain.repository.SearchRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {
    @Test
    fun `blank query clears results and skips repository call`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            try {
                val repository = FakeSearchRepository()
                val viewModel = SearchViewModel(repository)

                viewModel.setQuery("   ")
                advanceUntilIdle()

                assertTrue(viewModel.uiState.value.results.isEmpty())
                assertEquals(null, viewModel.uiState.value.error)
                assertEquals(0, repository.callCount)
            } finally {
                Dispatchers.resetMain()
            }
        }

    @Test
    fun `successful search publishes sorted results`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            try {
                val now = 1_700_000_000_000L
                val expected =
                    listOf(
                        SearchResult(
                            id = "tx-1",
                            source = SearchSource.TRANSACTION,
                            title = "Naivas",
                            subtitle = "Groceries",
                            timestamp = now,
                        ),
                        SearchResult(
                            id = "task-2",
                            source = SearchSource.TASK,
                            title = "Review plan",
                            subtitle = "Weekly review",
                            timestamp = now - 1_000L,
                        ),
                    )
                val repository = FakeSearchRepository(result = Result.success(expected))
                val viewModel = SearchViewModel(repository)

                viewModel.setQuery("review")
                advanceUntilIdle()

                val state = viewModel.uiState.value
                assertFalse(state.isLoading)
                assertEquals(expected, state.results)
                assertEquals(null, state.error)
                assertEquals(1, repository.callCount)
                assertEquals("review", repository.lastQuery)
            } finally {
                Dispatchers.resetMain()
            }
        }

    @Test
    fun `repository failure maps to explicit error`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            try {
                val repository =
                    FakeSearchRepository(
                        result = Result.failure(IllegalStateException("search backend down")),
                    )
                val viewModel = SearchViewModel(repository)

                viewModel.setQuery("finance")
                advanceUntilIdle()

                val state = viewModel.uiState.value
                assertFalse(state.isLoading)
                assertTrue(state.results.isEmpty())
                assertEquals("search backend down", state.error)
                assertEquals(1, repository.callCount)
            } finally {
                Dispatchers.resetMain()
            }
        }
}

private class FakeSearchRepository(
    var result: Result<List<SearchResult>> = Result.success(emptyList()),
) : SearchRepository {
    var callCount: Int = 0
    var lastQuery: String? = null

    override suspend fun search(
        query: String,
        limitPerSource: Int,
    ): List<SearchResult> {
        callCount += 1
        lastQuery = query
        return result.getOrThrow()
    }
}
