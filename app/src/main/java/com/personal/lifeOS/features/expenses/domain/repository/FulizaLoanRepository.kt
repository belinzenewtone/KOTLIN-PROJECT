package com.personal.lifeOS.features.expenses.domain.repository

import kotlinx.coroutines.flow.Flow

interface FulizaLoanRepository {
    /** Net outstanding Fuliza balance (sum of all open/partially-repaid loans). */
    fun observeNetOutstanding(): Flow<Double>

    /** Observe all open or partially-repaid Fuliza loans. */
    fun observeOpenLoans(): Flow<List<com.personal.lifeOS.core.database.entity.FulizaLoanEntity>>

    /** Record a new Fuliza draw (when a LOAN transaction is imported). */
    suspend fun recordDraw(drawCode: String, amountKes: Double, drawDate: Long)

    /** Record a repayment against an existing draw. */
    suspend fun recordRepayment(drawCode: String, repaidAmountKes: Double, repaymentDate: Long)
}
