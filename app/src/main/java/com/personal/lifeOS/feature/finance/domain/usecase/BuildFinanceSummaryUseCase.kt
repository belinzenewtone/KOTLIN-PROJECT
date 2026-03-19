package com.personal.lifeOS.feature.finance.domain.usecase

import com.personal.lifeOS.feature.finance.domain.model.FinanceCategoryBreakdown
import com.personal.lifeOS.feature.finance.domain.model.FinanceSnapshot
import com.personal.lifeOS.feature.finance.domain.model.FinanceSpendingSummary
import com.personal.lifeOS.feature.finance.domain.model.FinanceTransaction
import com.personal.lifeOS.feature.finance.domain.model.FinanceTransactionFilter
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject

class BuildFinanceSummaryUseCase
    @Inject
    constructor() {
        operator fun invoke(
            snapshot: FinanceSnapshot,
            referenceTimeMillis: Long = System.currentTimeMillis(),
        ): FinanceSpendingSummary {
            val todayRange = DateRange.forFilter(FinanceTransactionFilter.TODAY, referenceTimeMillis)
            val weekRange = DateRange.forFilter(FinanceTransactionFilter.THIS_WEEK, referenceTimeMillis)
            val monthRange = DateRange.forFilter(FinanceTransactionFilter.THIS_MONTH, referenceTimeMillis)
            val monthTransactions =
                snapshot.transactions.filter { tx ->
                    tx.date in monthRange.startMillis..monthRange.endMillis
                }
            val monthTotal = monthTransactions.sumOf(FinanceTransaction::amount)
            val categoryBreakdown = monthTransactions.toCategoryBreakdown(monthTotal)
            val topMerchant =
                monthTransactions
                    .groupBy(FinanceTransaction::merchant)
                    .maxByOrNull { (_, merchantTransactions) ->
                        merchantTransactions.sumOf(FinanceTransaction::amount)
                    }?.key

            return FinanceSpendingSummary(
                todayTotal = snapshot.transactions.sumOfInRange(todayRange),
                weekTotal = snapshot.transactions.sumOfInRange(weekRange),
                monthTotal = monthTotal,
                transactionCount = snapshot.transactions.size,
                categoryBreakdown = categoryBreakdown,
                topMerchant = topMerchant,
            )
        }
    }

class FilterFinanceTransactionsUseCase
    @Inject
    constructor() {
        operator fun invoke(
            snapshot: FinanceSnapshot,
            filter: FinanceTransactionFilter,
            referenceTimeMillis: Long = System.currentTimeMillis(),
        ): List<FinanceTransaction> {
            if (filter == FinanceTransactionFilter.ALL) {
                return snapshot.transactions.sortedByDescending(FinanceTransaction::date)
            }
            val range = DateRange.forFilter(filter, referenceTimeMillis)
            return snapshot.transactions
                .asSequence()
                .filter { tx -> tx.date in range.startMillis..range.endMillis }
                .sortedByDescending(FinanceTransaction::date)
                .toList()
        }
    }

private fun List<FinanceTransaction>.sumOfInRange(range: DateRange): Double {
    return asSequence()
        .filter { tx -> tx.date in range.startMillis..range.endMillis }
        .sumOf(FinanceTransaction::amount)
}

private fun List<FinanceTransaction>.toCategoryBreakdown(monthTotal: Double): List<FinanceCategoryBreakdown> {
    return groupBy { tx -> tx.category.ifBlank { "Uncategorized" } }
        .map { (category, categoryTransactions) ->
            FinanceCategoryBreakdown(
                category = category,
                total = categoryTransactions.sumOf(FinanceTransaction::amount),
            )
        }.sortedByDescending(FinanceCategoryBreakdown::total)
        .map { breakdown ->
            val percentage =
                if (monthTotal <= 0.0) {
                    0f
                } else {
                    ((breakdown.total / monthTotal) * 100).toFloat()
                }
            breakdown.copy(percentage = percentage)
        }
}

private data class DateRange(
    val startMillis: Long,
    val endMillis: Long,
) {
    companion object {
        fun forFilter(
            filter: FinanceTransactionFilter,
            referenceTimeMillis: Long,
        ): DateRange {
            val zone = ZoneId.systemDefault()
            val date = Instant.ofEpochMilli(referenceTimeMillis).atZone(zone).toLocalDate()

            return when (filter) {
                FinanceTransactionFilter.ALL -> DateRange(Long.MIN_VALUE, Long.MAX_VALUE)
                FinanceTransactionFilter.TODAY -> from(date, date.plusDays(1), zone)
                FinanceTransactionFilter.THIS_WEEK ->
                    from(
                        date.with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY)),
                        date.plusDays(1),
                        zone,
                    )
                FinanceTransactionFilter.THIS_MONTH ->
                    from(
                        date.withDayOfMonth(1),
                        date.with(TemporalAdjusters.lastDayOfMonth()).plusDays(1),
                        zone,
                    )
            }
        }

        private fun from(
            startDate: LocalDate,
            exclusiveEndDate: LocalDate,
            zone: ZoneId,
        ): DateRange {
            val start = startDate.atStartOfDay(zone).toInstant().toEpochMilli()
            val end = exclusiveEndDate.atStartOfDay(zone).toInstant().toEpochMilli() - 1
            return DateRange(startMillis = start, endMillis = end)
        }
    }
}
