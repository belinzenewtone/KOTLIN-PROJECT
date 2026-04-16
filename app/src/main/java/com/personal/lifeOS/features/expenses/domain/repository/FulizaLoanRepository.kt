package com.personal.lifeOS.features.expenses.domain.repository

import kotlinx.coroutines.flow.Flow

interface FulizaLoanRepository {
    /**
     * Reactive outstanding Fuliza balance.
     * Returns the SMS-derived balance from [setOutstandingKes] when available,
     * otherwise 0.0 (the FIFO SQL approach is retired — balance is now set directly from SMS).
     */
    fun observeNetOutstanding(): Flow<Double>

    /** Observe all open or partially-repaid Fuliza loans. */
    fun observeOpenLoans(): Flow<List<com.personal.lifeOS.core.database.entity.FulizaLoanEntity>>

    /** Record a new Fuliza draw (when a LOAN transaction is imported). */
    suspend fun recordDraw(drawCode: String, amountKes: Double, drawDate: Long)

    /** Record a repayment against an existing draw. */
    suspend fun recordRepayment(drawCode: String, repaidAmountKes: Double, repaymentDate: Long)

    /**
     * Persist the definitive Fuliza outstanding balance sourced directly from an SMS:
     * - FULIZA_CHARGE SMS: outstanding stated explicitly as "Total Fuliza … outstanding amount is Ksh X"
     * - LOAN repayment SMS: outstanding = userFulizaLimit - availableLimitFromSms
     *
     * This replaces the FIFO reconciliation model. Callers should always prefer the
     * SMS-stated values over any computed estimate.
     */
    suspend fun setOutstandingKes(outstandingKes: Double)
}
