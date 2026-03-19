package com.personal.lifeOS.features.budget.domain.usecase

import com.personal.lifeOS.features.budget.domain.model.Budget
import com.personal.lifeOS.features.budget.domain.repository.BudgetRepository
import javax.inject.Inject

class AddBudgetUseCase
    @Inject
    constructor(
        private val budgetRepository: BudgetRepository,
    ) {
        suspend operator fun invoke(budget: Budget): Long {
            return budgetRepository.addBudget(budget)
        }
    }
