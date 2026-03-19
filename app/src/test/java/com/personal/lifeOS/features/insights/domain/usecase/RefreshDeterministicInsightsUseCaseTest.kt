package com.personal.lifeOS.features.insights.domain.usecase

import com.personal.lifeOS.features.insights.domain.model.InsightCard
import com.personal.lifeOS.features.insights.domain.repository.InsightRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class RefreshDeterministicInsightsUseCaseTest {
    @Test
    fun `invoke delegates to repository refresh`() =
        runTest {
            val repository = FakeInsightRepository()
            val useCase = RefreshDeterministicInsightsUseCase(repository)

            useCase()

            assertEquals(1, repository.refreshCallCount)
        }
}

private class FakeInsightRepository : InsightRepository {
    var refreshCallCount: Int = 0

    override fun observeCards(): Flow<List<InsightCard>> = flowOf(emptyList())

    override suspend fun refreshDeterministicCards(now: Long) {
        refreshCallCount += 1
    }
}
