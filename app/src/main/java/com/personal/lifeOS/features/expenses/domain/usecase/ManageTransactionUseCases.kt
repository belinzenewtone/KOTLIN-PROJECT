package com.personal.lifeOS.features.expenses.domain.usecase

import com.personal.lifeOS.features.expenses.domain.model.Transaction
import com.personal.lifeOS.features.expenses.domain.repository.ExpenseRepository
import javax.inject.Inject

class DeleteTransactionUseCase @Inject constructor(
    private val repository: ExpenseRepository
) {
    suspend operator fun invoke(transaction: Transaction) {
        repository.deleteTransaction(transaction)
    }
}

class UpdateMerchantCategoryUseCase @Inject constructor(
    private val repository: ExpenseRepository
) {
    /**
     * Updates the category for a merchant. This correction is learned
     * and applied to future transactions from the same merchant.
     */
    suspend operator fun invoke(merchant: String, newCategory: String) {
        repository.updateMerchantCategory(merchant, newCategory)
    }
}
