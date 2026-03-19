package com.personal.lifeOS.features.export.data.repository

import com.personal.lifeOS.core.database.entity.BudgetEntity
import com.personal.lifeOS.core.database.entity.EventEntity
import com.personal.lifeOS.core.database.entity.IncomeEntity
import com.personal.lifeOS.core.database.entity.MerchantCategoryEntity
import com.personal.lifeOS.core.database.entity.RecurringRuleEntity
import com.personal.lifeOS.core.database.entity.TaskEntity
import com.personal.lifeOS.core.database.entity.TransactionEntity
import com.personal.lifeOS.features.export.domain.model.ExportDomain

internal data class ExportBundle(
    val transactions: List<TransactionEntity> = emptyList(),
    val tasks: List<TaskEntity> = emptyList(),
    val events: List<EventEntity> = emptyList(),
    val budgets: List<BudgetEntity> = emptyList(),
    val incomes: List<IncomeEntity> = emptyList(),
    val recurringRules: List<RecurringRuleEntity> = emptyList(),
    val merchantRules: List<MerchantCategoryEntity> = emptyList(),
) {
    fun totalItemCount(): Int {
        return listOf(
            transactions.size,
            tasks.size,
            events.size,
            budgets.size,
            incomes.size,
            recurringRules.size,
            merchantRules.size,
        ).sum()
    }

    fun domainCounts(): Map<ExportDomain, Int> {
        return linkedMapOf(
            ExportDomain.TRANSACTIONS to transactions.size,
            ExportDomain.TASKS to tasks.size,
            ExportDomain.EVENTS to events.size,
            ExportDomain.BUDGETS to budgets.size,
            ExportDomain.INCOMES to incomes.size,
            ExportDomain.RECURRING_RULES to recurringRules.size,
            ExportDomain.MERCHANT_RULES to merchantRules.size,
        )
    }

    fun select(domain: ExportDomain): ExportBundle {
        return when (domain) {
            ExportDomain.ALL -> this
            ExportDomain.TRANSACTIONS -> ExportBundle(transactions = transactions)
            ExportDomain.TASKS -> ExportBundle(tasks = tasks)
            ExportDomain.EVENTS -> ExportBundle(events = events)
            ExportDomain.BUDGETS -> ExportBundle(budgets = budgets)
            ExportDomain.INCOMES -> ExportBundle(incomes = incomes)
            ExportDomain.RECURRING_RULES -> ExportBundle(recurringRules = recurringRules)
            ExportDomain.MERCHANT_RULES -> ExportBundle(merchantRules = merchantRules)
        }
    }
}
