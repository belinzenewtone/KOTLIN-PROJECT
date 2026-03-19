package com.personal.lifeOS.features.insights.domain.service

import com.personal.lifeOS.features.insights.domain.model.DeterministicInsightInput
import com.personal.lifeOS.features.insights.domain.model.InsightTaskSnapshot
import com.personal.lifeOS.features.insights.domain.model.InsightTransactionSnapshot
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration

class DeterministicInsightEngineTest {
    private val engine = DeterministicInsightEngine()

    @Test
    fun `build creates overdue tasks insight when overdue tasks exist`() {
        val now = System.currentTimeMillis()

        val insights =
            engine.build(
                DeterministicInsightInput(
                    nowMillis = now,
                    pendingTasks =
                        listOf(
                            InsightTaskSnapshot(
                                title = "Submit taxes",
                                deadline = now - Duration.ofHours(2).toMillis(),
                            ),
                            InsightTaskSnapshot(
                                title = "New task",
                                deadline = now + Duration.ofDays(1).toMillis(),
                            ),
                        ),
                    recentTransactions = emptyList(),
                ),
            )

        assertTrue(insights.any { it.kind == "DETERMINISTIC_OVERDUE_TASKS" })
    }

    @Test
    fun `build creates spending acceleration insight when recent week rises`() {
        val now = System.currentTimeMillis()
        val day = Duration.ofDays(1).toMillis()

        val insights =
            engine.build(
                DeterministicInsightInput(
                    nowMillis = now,
                    pendingTasks = emptyList(),
                    recentTransactions =
                        listOf(
                            InsightTransactionSnapshot(
                                amount = 2_200.0,
                                category = "Groceries",
                                date = now - (2 * day),
                            ),
                            InsightTransactionSnapshot(
                                amount = 1_800.0,
                                category = "Groceries",
                                date = now - (4 * day),
                            ),
                            InsightTransactionSnapshot(
                                amount = 1_000.0,
                                category = "Groceries",
                                date = now - (10 * day),
                            ),
                            InsightTransactionSnapshot(
                                amount = 900.0,
                                category = "Transport",
                                date = now - (12 * day),
                            ),
                        ),
                ),
            )

        assertTrue(insights.any { it.kind == "DETERMINISTIC_SPENDING_ACCELERATION" })
    }

    @Test
    fun `build creates category pressure insight when one category dominates month`() {
        val now = System.currentTimeMillis()
        val day = Duration.ofDays(1).toMillis()

        val insights =
            engine.build(
                DeterministicInsightInput(
                    nowMillis = now,
                    pendingTasks = emptyList(),
                    recentTransactions =
                        listOf(
                            InsightTransactionSnapshot(
                                amount = 3_500.0,
                                category = "Food",
                                date = now - (2 * day),
                            ),
                            InsightTransactionSnapshot(
                                amount = 2_200.0,
                                category = "Food",
                                date = now - (5 * day),
                            ),
                            InsightTransactionSnapshot(
                                amount = 1_100.0,
                                category = "Transport",
                                date = now - (7 * day),
                            ),
                        ),
                ),
            )

        assertTrue(insights.any { it.kind == "DETERMINISTIC_CATEGORY_PRESSURE" })
    }
}
