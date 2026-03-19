package com.personal.lifeOS.features.budget.domain.usecase

import com.personal.lifeOS.features.budget.domain.repository.BudgetRepository
import javax.inject.Inject

class DeleteBudgetUseCase
    @Inject
    constructor(
        private val budgetRepository: BudgetRepository,
    ) {
        suspend operator fun invoke(id: Long) {
            budgetRepository.deleteBudget(id)
        }
    }
