package com.personal.lifeOS.features.analytics.presentation

import com.personal.lifeOS.features.analytics.domain.model.AnalyticsData
import com.personal.lifeOS.features.analytics.domain.model.AnalyticsPeriod
import com.personal.lifeOS.features.analytics.domain.model.DailySpending
import com.personal.lifeOS.features.analytics.domain.repository.AnalyticsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AnalyticsViewModelTest {
    @Test
    fun `analytics data emission updates ui state and clears loading`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            try {
                val expected =
                    AnalyticsData(
                        weeklySpending =
                            listOf(
                                DailySpending(
                                    dayLabel = "Mon",
                                    amount = 320.0,
                                    date = 1_700_000_000_000L,
                                ),
                            ),
                        totalSpentThisMonth = 8_400.0,
                        totalTasksCompleted = 12,
                    )
                val viewModel =
                    AnalyticsViewModel(
                        repository = FakeAnalyticsRepository(flowOf(expected)),
                    )

                advanceUntilIdle()
                val state = viewModel.uiState.value
                assertFalse(state.isLoading)
                assertEquals(expected, state.data)
                assertEquals(null, state.error)
            } finally {
                Dispatchers.resetMain()
            }
        }

    @Test
    fun `repository failure maps to explicit ui error`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            try {
                val viewModel =
                    AnalyticsViewModel(
                        repository =
                            FakeAnalyticsRepository(
                                flow {
                                    throw IllegalStateException("analytics unavailable")
                                },
                            ),
                    )

                advanceUntilIdle()
                val state = viewModel.uiState.value
                assertFalse(state.isLoading)
                assertEquals("analytics unavailable", state.error)
            } finally {
                Dispatchers.resetMain()
            }
        }

    @Test
    fun `set period updates selector state`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            try {
                val viewModel = AnalyticsViewModel(repository = FakeAnalyticsRepository(flowOf(AnalyticsData())))

                viewModel.setPeriod(AnalyticsPeriod.MONTH)

                assertEquals(AnalyticsPeriod.MONTH, viewModel.uiState.value.selectedPeriod)
            } finally {
                Dispatchers.resetMain()
            }
        }
}

private class FakeAnalyticsRepository(
    private val stream: Flow<AnalyticsData>,
) : AnalyticsRepository {
    override fun getAnalytics(): Flow<AnalyticsData> = stream
}
