package com.personal.lifeOS.feature.finance.presentation

import com.personal.lifeOS.core.telemetry.ImportHealthSummary
import com.personal.lifeOS.core.ui.model.ImportHealthUiModel
import com.personal.lifeOS.feature.finance.domain.model.FinanceSnapshot
import com.personal.lifeOS.feature.finance.domain.model.FinanceTransaction
import com.personal.lifeOS.feature.finance.domain.model.FinanceTransactionFilter
import com.personal.lifeOS.feature.finance.domain.usecase.BuildFinanceSummaryUseCase
import com.personal.lifeOS.feature.finance.domain.usecase.FilterFinanceTransactionsUseCase
import com.personal.lifeOS.features.budget.domain.model.Budget
import com.personal.lifeOS.features.budget.domain.model.BudgetPeriod
import com.personal.lifeOS.features.income.domain.model.IncomeRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class FinanceContractsTest {
    @Test
    fun `spending summary use case derives finance totals from snapshot`() {
        val reference = localMillis(2026, 3, 19, 9, 0)
        val snapshot =
            FinanceSnapshot(
                transactions =
                    listOf(
                        FinanceTransaction(
                            id = 1,
                            amount = 1200.0,
                            merchant = "Naivas",
                            category = "Groceries",
                            date = localMillis(2026, 3, 19, 8, 0),
                        ),
                        FinanceTransaction(
                            id = 2,
                            amount = 500.0,
                            merchant = "Bus",
                            category = "Transport",
                            date = localMillis(2026, 3, 18, 18, 0),
                        ),
                        FinanceTransaction(
                            id = 3,
                            amount = 900.0,
                            merchant = "Naivas",
                            category = "Groceries",
                            date = localMillis(2026, 3, 2, 10, 0),
                        ),
                    ),
                budgets =
                    listOf(
                        Budget(
                            id = 1,
                            category = "GROCERIES",
                            limitAmount = 5_000.0,
                            period = BudgetPeriod.MONTHLY,
                        ),
                    ),
                incomes =
                    listOf(
                        IncomeRecord(
                            id = 10,
                            amount = 10_000.0,
                            source = "Salary",
                            date = localMillis(2026, 3, 1, 9, 0),
                        ),
                    ),
            )

        val summary = BuildFinanceSummaryUseCase()(snapshot, reference)

        assertEquals(1200.0, summary.todayTotal, 0.0)
        assertEquals(1700.0, summary.weekTotal, 0.0)
        assertEquals(2600.0, summary.monthTotal, 0.0)
        assertEquals(3, summary.transactionCount)
        assertEquals("Naivas", summary.topMerchant)
        assertEquals(2, summary.categoryBreakdown.size)
    }

    @Test
    fun `filter transactions use case honors selected filter and sorting`() {
        val reference = localMillis(2026, 3, 19, 9, 0)
        val older = localMillis(2026, 3, 1, 12, 0)
        val todayMorning = localMillis(2026, 3, 19, 7, 30)
        val todayEvening = localMillis(2026, 3, 19, 20, 0)

        val snapshot =
            FinanceSnapshot(
                transactions =
                    listOf(
                        FinanceTransaction(1, 200.0, "Shop", "Misc", older),
                        FinanceTransaction(2, 300.0, "Fuel", "Transport", todayMorning),
                        FinanceTransaction(3, 400.0, "Cafe", "Food", todayEvening),
                    ),
            )

        val todayTransactions =
            FilterFinanceTransactionsUseCase()(
                snapshot = snapshot,
                filter = FinanceTransactionFilter.TODAY,
                referenceTimeMillis = reference,
            )

        assertEquals(2, todayTransactions.size)
        assertEquals(3, todayTransactions[0].id)
        assertEquals(2, todayTransactions[1].id)
    }

    @Test
    fun `import health summary maps to cross-feature ui model`() {
        val model: ImportHealthUiModel =
            ImportHealthSummary(
                imported = 9,
                duplicate = 2,
                parseFailed = 1,
                pending = 4,
                recovered = 3,
                latestImportAt = 1_700_000_000_000L,
            ).toUiModel(lastImportRunSummary = null)

        assertEquals(9, model.importedCount)
        assertEquals(4, model.pendingReviewCount)
        assertEquals(2, model.duplicateCount)
        assertEquals(1, model.parseFailureCount)
        assertEquals(3, model.recoveredCount)
        assertTrue(model.lastImportSummary?.contains("Last import") == true)
        assertEquals(1_700_000_000_000L, model.latestImportAt)
    }

    @Test
    fun `finance helpers derive freshness review queue and budget guardrails`() {
        val reference = localMillis(2026, 3, 19, 9, 0)
        val snapshot =
            FinanceSnapshot(
                transactions =
                    listOf(
                        FinanceTransaction(
                            id = 1,
                            amount = 4_500.0,
                            merchant = "Naivas",
                            category = "Groceries",
                            date = localMillis(2026, 3, 19, 8, 0),
                        ),
                    ),
                budgets =
                    listOf(
                        Budget(
                            id = 1,
                            category = "Groceries",
                            limitAmount = 5_000.0,
                            period = BudgetPeriod.MONTHLY,
                        ),
                    ),
            )
        val importHealth =
            ImportHealthSummary(
                pending = 2,
                latestImportAt = reference,
            )
        val summary = BuildFinanceSummaryUseCase()(snapshot, reference)

        val freshness = buildFinanceFreshness(snapshot, importHealth, lastSyncAt = null)
        val reviewQueue = buildReviewQueueSummary(importHealth)
        val guardrail = buildBudgetGuardrail(summary, totalMonthBudget = 5_000.0)

        assertNotNull(freshness)
        assertEquals("2 transactions waiting for review", reviewQueue)
        assertNotNull(guardrail)
        assertTrue(guardrail?.message?.contains("%") == true || guardrail?.message?.contains("over") == true)
    }
}

private fun localMillis(
    year: Int,
    month: Int,
    day: Int,
    hour: Int,
    minute: Int,
): Long {
    return LocalDateTime.of(year, month, day, hour, minute)
        .atZone(ZoneId.systemDefault())
        .toInstant()
        .toEpochMilli()
}
