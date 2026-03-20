package com.personal.lifeOS.features.insights.domain.service

import com.personal.lifeOS.features.insights.domain.model.DeterministicInsightDraft
import com.personal.lifeOS.features.insights.domain.model.DeterministicInsightInput
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToLong

@Singleton
class DeterministicInsightEngine
    @Inject
    constructor() {
        fun build(input: DeterministicInsightInput): List<DeterministicInsightDraft> {
            val insights = mutableListOf<DeterministicInsightDraft>()

            buildOverdueTasksInsight(input)?.let(insights::add)
            buildSpendingSummaryInsight(input)?.let(insights::add)
            buildSpendingAccelerationInsight(input)?.let(insights::add)
            buildCategoryPressureInsight(input)?.let(insights::add)

            return insights
        }

        private fun buildOverdueTasksInsight(input: DeterministicInsightInput): DeterministicInsightDraft? {
            val overdueTasks =
                input.pendingTasks.filter { snapshot ->
                    snapshot.deadline != null && snapshot.deadline < input.nowMillis
                }
            if (overdueTasks.isEmpty()) {
                return null
            }

            val firstTitle = overdueTasks.firstOrNull()?.title?.takeIf { it.isNotBlank() }
            val nextAction =
                if (firstTitle != null) {
                    "Start with \"$firstTitle\"."
                } else {
                    "Pick one to clear first."
                }

            return DeterministicInsightDraft(
                kind = "DETERMINISTIC_OVERDUE_TASKS",
                title = "Overdue tasks need attention",
                body = "You have ${overdueTasks.size} overdue tasks. $nextAction",
                confidence = 1.0,
            )
        }

        /**
         * Always fires when the user has ANY spending data for the current month.
         * Gives them something to see immediately rather than the "warming up" state.
         */
        private fun buildSpendingSummaryInsight(input: DeterministicInsightInput): DeterministicInsightDraft? {
            val zoneId = ZoneId.systemDefault()
            val monthStart =
                Instant.ofEpochMilli(input.nowMillis)
                    .atZone(zoneId).toLocalDate().withDayOfMonth(1)
                    .atStartOfDay(zoneId).toInstant().toEpochMilli()

            val monthlyTx = input.recentTransactions.filter { it.date >= monthStart }
            if (monthlyTx.isEmpty()) return null

            val monthlyTotal = monthlyTx.sumOf { it.amount }
            val categoryTotals = monthlyTx.groupingBy { it.category }.fold(0.0) { acc, tx -> acc + tx.amount }
            val topCategory = categoryTotals.maxByOrNull { it.value }

            val topLine = if (topCategory != null) {
                val share = (topCategory.value / monthlyTotal * 100).roundToLong()
                "${topCategory.key} leads at $share% of spend."
            } else {
                "Keep tracking to unlock category breakdowns."
            }

            return DeterministicInsightDraft(
                kind = "DETERMINISTIC_SPENDING_SUMMARY",
                title = "Month so far: ${formatCurrency(monthlyTotal)}",
                body = "You've recorded ${monthlyTx.size} transaction${if (monthlyTx.size == 1) "" else "s"} this month. $topLine",
                confidence = 1.0,
            )
        }

        private fun buildSpendingAccelerationInsight(input: DeterministicInsightInput): DeterministicInsightDraft? {
            val now = Instant.ofEpochMilli(input.nowMillis)
            val lastSevenDaysStart = now.minus(7, ChronoUnit.DAYS).toEpochMilli()
            val previousSevenDaysStart = now.minus(14, ChronoUnit.DAYS).toEpochMilli()

            val currentWindowSpend =
                input.recentTransactions
                    .filter { it.date in lastSevenDaysStart..input.nowMillis }
                    .sumOf { it.amount }

            val previousWindowSpend =
                input.recentTransactions
                    .filter { it.date in previousSevenDaysStart until lastSevenDaysStart }
                    .sumOf { it.amount }

            if (previousWindowSpend <= 0.0 || currentWindowSpend <= 0.0) {
                return null
            }

            val growthRatio = currentWindowSpend / previousWindowSpend
            val delta = currentWindowSpend - previousWindowSpend
            // Lowered delta threshold so small-data users see acceleration insights sooner
            if (growthRatio < 1.2 || delta < 100.0) {
                return null
            }

            val increasePercent = ((growthRatio - 1.0) * 100.0).roundToLong()
            return DeterministicInsightDraft(
                kind = "DETERMINISTIC_SPENDING_ACCELERATION",
                title = "Spending is accelerating",
                body =
                    "Last 7 days rose by $increasePercent% " +
                        "(${formatCurrency(currentWindowSpend)} vs ${formatCurrency(previousWindowSpend)}).",
                confidence = 0.84,
            )
        }

        private fun buildCategoryPressureInsight(input: DeterministicInsightInput): DeterministicInsightDraft? {
            val zoneId = ZoneId.systemDefault()
            val monthStart =
                Instant.ofEpochMilli(input.nowMillis)
                    .atZone(zoneId)
                    .toLocalDate()
                    .withDayOfMonth(1)
                    .atStartOfDay(zoneId)
                    .toInstant()
                    .toEpochMilli()

            val monthlyTransactions = input.recentTransactions.filter { it.date >= monthStart }
            if (monthlyTransactions.isEmpty()) {
                return null
            }

            val monthlyTotal = monthlyTransactions.sumOf { it.amount }
            // Lowered from 3000 so users with even a few hundred KES see category pressure
            if (monthlyTotal < 500.0) {
                return null
            }

            val categoryTotals = monthlyTransactions.groupingBy { it.category }.fold(0.0) { acc, tx -> acc + tx.amount }
            val dominantCategory = categoryTotals.maxByOrNull { it.value } ?: return null
            val dominantShare = dominantCategory.value / monthlyTotal
            if (dominantShare < 0.55) {
                return null
            }

            val sharePercent = (dominantShare * 100).roundToLong()
            return DeterministicInsightDraft(
                kind = "DETERMINISTIC_CATEGORY_PRESSURE",
                title = "${dominantCategory.key} is driving your spend",
                body = "$sharePercent% of this month's spending is in ${dominantCategory.key}.",
                confidence = 0.78,
            )
        }

        private fun formatCurrency(amount: Double): String {
            return "KES ${String.format(Locale.US, "%,.0f", amount)}"
        }
    }
