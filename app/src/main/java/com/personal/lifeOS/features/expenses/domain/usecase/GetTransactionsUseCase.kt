package com.personal.lifeOS.features.expenses.domain.usecase

import com.personal.lifeOS.core.utils.DateUtils
import com.personal.lifeOS.features.expenses.domain.model.Transaction
import com.personal.lifeOS.features.expenses.domain.model.TransactionFilter
import com.personal.lifeOS.features.expenses.domain.repository.ExpenseRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetTransactionsUseCase
    @Inject
    constructor(
        private val repository: ExpenseRepository,
    ) {
        operator fun invoke(filter: TransactionFilter = TransactionFilter.ALL): Flow<List<Transaction>> {
            return when (filter) {
                TransactionFilter.ALL -> repository.getAllTransactions()
                TransactionFilter.TODAY ->
                    repository.getTransactionsBetween(
                        DateUtils.todayStartMillis(),
                        DateUtils.todayEndMillis(),
                    )
                TransactionFilter.THIS_WEEK ->
                    repository.getTransactionsBetween(
                        DateUtils.weekStartMillis(),
                        DateUtils.todayEndMillis(),
                    )
                TransactionFilter.THIS_MONTH ->
                    repository.getTransactionsBetween(
                        DateUtils.monthStartMillis(),
                        DateUtils.monthEndMillis(),
                    )
            }
        }

        fun byCategory(category: String): Flow<List<Transaction>> {
            return repository.getByCategory(category)
        }
    }
