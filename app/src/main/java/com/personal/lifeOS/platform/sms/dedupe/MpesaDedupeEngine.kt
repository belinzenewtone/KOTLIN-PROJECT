package com.personal.lifeOS.platform.sms.dedupe

import com.personal.lifeOS.features.expenses.domain.repository.ExpenseRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MpesaDedupeEngine
    @Inject
    constructor(
        private val expenseRepository: ExpenseRepository,
    ) {
        suspend fun isDuplicate(
            mpesaCode: String,
            amount: Double,
            merchant: String,
            timestamp: Long,
        ): Boolean {
            if (expenseRepository.existsByMpesaCode(mpesaCode)) {
                return true
            }

            return expenseRepository.existsPotentialDuplicate(
                amount = amount,
                merchant = merchant,
                date = timestamp,
            )
        }
    }
