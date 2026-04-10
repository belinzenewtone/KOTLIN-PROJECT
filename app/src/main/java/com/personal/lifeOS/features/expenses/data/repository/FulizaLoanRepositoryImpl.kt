package com.personal.lifeOS.features.expenses.data.repository

import com.personal.lifeOS.core.database.LocalIdGenerator
import com.personal.lifeOS.core.database.dao.FulizaLoanDao
import com.personal.lifeOS.core.database.entity.FulizaLoanEntity
import com.personal.lifeOS.core.database.entity.FulizaLoanStatus
import com.personal.lifeOS.core.security.AuthSessionStore
import com.personal.lifeOS.features.expenses.domain.repository.FulizaLoanRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FulizaLoanRepositoryImpl
    @Inject
    constructor(
        private val fulizaLoanDao: FulizaLoanDao,
        private val authSessionStore: AuthSessionStore,
    ) : FulizaLoanRepository {

        private fun userId(): String = authSessionStore.getUserId()

        override fun observeNetOutstanding(): Flow<Double> =
            fulizaLoanDao.observeNetOutstanding(userId())

        override fun observeOpenLoans(): Flow<List<FulizaLoanEntity>> =
            fulizaLoanDao.observeOpenLoans(userId())

        override suspend fun recordDraw(
            drawCode: String,
            amountKes: Double,
            drawDate: Long,
        ) {
            val existing = fulizaLoanDao.getByDrawCode(drawCode, userId())
            if (existing != null) return  // already recorded, skip

            fulizaLoanDao.insert(
                FulizaLoanEntity(
                    id = LocalIdGenerator.nextId(),
                    drawCode = drawCode,
                    drawAmountKes = amountKes,
                    totalRepaidKes = 0.0,
                    status = FulizaLoanStatus.OPEN.name,
                    drawDate = drawDate,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis(),
                    userId = userId(),
                ),
            )
        }

        override suspend fun recordRepayment(
            drawCode: String,
            repaidAmountKes: Double,
            repaymentDate: Long,
        ) {
            if (repaidAmountKes <= 0.0) return

            val existing = fulizaLoanDao.getByDrawCode(drawCode, userId())
            if (existing != null) {
                val newRepaid = existing.totalRepaidKes + repaidAmountKes
                val newStatus = when {
                    newRepaid >= existing.drawAmountKes -> FulizaLoanStatus.CLOSED
                    newRepaid > 0 -> FulizaLoanStatus.PARTIALLY_REPAID
                    else -> FulizaLoanStatus.OPEN
                }
                fulizaLoanDao.update(
                    existing.copy(
                        totalRepaidKes = newRepaid.coerceAtMost(existing.drawAmountKes),
                        status = newStatus.name,
                        lastRepaymentDate = repaymentDate,
                        updatedAt = System.currentTimeMillis(),
                    ),
                )
                return
            }

            // Safaricom repayment SMS codes are often different from the original draw code.
            // Fall back to reducing oldest open draws first (FIFO).
            var remaining = repaidAmountKes
            val now = System.currentTimeMillis()
            val openLoans = fulizaLoanDao.getOpenLoansOldestFirst(userId())
            for (loan in openLoans) {
                if (remaining <= 0.0) break
                val outstanding = (loan.drawAmountKes - loan.totalRepaidKes).coerceAtLeast(0.0)
                if (outstanding > 0.0) {
                    val applied = minOf(outstanding, remaining)
                    val updatedRepaid = loan.totalRepaidKes + applied
                    val updatedStatus = when {
                        updatedRepaid >= loan.drawAmountKes -> FulizaLoanStatus.CLOSED
                        updatedRepaid > 0.0 -> FulizaLoanStatus.PARTIALLY_REPAID
                        else -> FulizaLoanStatus.OPEN
                    }
                    fulizaLoanDao.update(
                        loan.copy(
                            totalRepaidKes = updatedRepaid.coerceAtMost(loan.drawAmountKes),
                            status = updatedStatus.name,
                            lastRepaymentDate = repaymentDate,
                            updatedAt = now,
                        ),
                    )
                    remaining -= applied
                }
            }
        }
    }
