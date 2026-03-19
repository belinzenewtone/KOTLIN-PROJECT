package com.personal.lifeOS.features.income.domain.usecase

import com.personal.lifeOS.core.utils.DateUtils
import com.personal.lifeOS.features.income.domain.model.IncomeRecord
import com.personal.lifeOS.features.income.domain.repository.IncomeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

data class IncomeSnapshot(
    val records: List<IncomeRecord> = emptyList(),
    val monthTotal: Double = 0.0,
)

class ObserveIncomeSnapshotUseCase
    @Inject
    constructor(
        private val incomeRepository: IncomeRepository,
    ) {
        operator fun invoke(): Flow<IncomeSnapshot> {
            return incomeRepository.getIncomes().map { incomes ->
                val monthStart = DateUtils.monthStartMillis()
                val monthEnd = DateUtils.monthEndMillis()

                IncomeSnapshot(
                    records = incomes,
                    monthTotal =
                        incomes
                            .asSequence()
                            .filter { income -> income.date in monthStart..monthEnd }
                            .sumOf(IncomeRecord::amount),
                )
            }
        }
    }
