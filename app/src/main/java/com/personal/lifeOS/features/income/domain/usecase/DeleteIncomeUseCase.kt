package com.personal.lifeOS.features.income.domain.usecase

import com.personal.lifeOS.features.income.domain.repository.IncomeRepository
import javax.inject.Inject

class DeleteIncomeUseCase
    @Inject
    constructor(
        private val incomeRepository: IncomeRepository,
    ) {
        suspend operator fun invoke(id: Long) {
            incomeRepository.deleteIncome(id)
        }
    }
