package com.personal.lifeOS.features.expenses.domain.usecase

import com.personal.lifeOS.features.expenses.domain.model.Transaction
import com.personal.lifeOS.features.expenses.domain.repository.ExpenseRepository
import javax.inject.Inject

class AddTransactionUseCase @Inject constructor(
    private val repository: ExpenseRepository
) {
    suspend operator fun invoke(transaction: Transaction): Long {
        return repository.addTransaction(transaction)
    }

    suspend fun fromSms(smsBody: String): Transaction? {
        return repository.importFromSms(smsBody)
    }
}
