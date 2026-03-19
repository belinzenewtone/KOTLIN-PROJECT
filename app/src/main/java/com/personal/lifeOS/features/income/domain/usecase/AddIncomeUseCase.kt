package com.personal.lifeOS.features.income.domain.usecase

import com.personal.lifeOS.features.income.domain.model.IncomeRecord
import com.personal.lifeOS.features.income.domain.repository.IncomeRepository
import javax.inject.Inject

class AddIncomeUseCase
    @Inject
    constructor(
        private val incomeRepository: IncomeRepository,
    ) {
        suspend operator fun invoke(record: IncomeRecord): Long {
            return incomeRepository.addIncome(record)
        }
    }
