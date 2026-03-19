package com.personal.lifeOS.features.income.domain.usecase

import com.personal.lifeOS.core.utils.DateUtils
import com.personal.lifeOS.features.income.domain.model.IncomeRecord
import com.personal.lifeOS.features.income.domain.repository.IncomeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ObserveIncomeSnapshotUseCaseTest {
    @Test
    fun `builds monthly total from current month income records`() =
        runTest {
            val startOfMonth = DateUtils.monthStartMillis()
            val repository =
                FakeIncomeRepository(
                    listOf(
                        IncomeRecord(
                            id = 1L,
                            amount = 100_000.0,
                            source = "Salary",
                            date = startOfMonth + 3_600_000L,
                        ),
                        IncomeRecord(
                            id = 2L,
                            amount = 12_500.0,
                            source = "Freelance",
                            date = startOfMonth + 86_400_000L,
                        ),
                        IncomeRecord(
                            id = 3L,
                            amount = 8_000.0,
                            source = "Old payout",
                            date = startOfMonth - 86_400_000L,
                        ),
                    ),
                )

            val snapshot = ObserveIncomeSnapshotUseCase(repository).invoke().first()

            assertEquals(3, snapshot.records.size)
            assertEquals(112_500.0, snapshot.monthTotal, 0.0)
        }
}

private class FakeIncomeRepository(
    initialRecords: List<IncomeRecord>,
) : IncomeRepository {
    private val recordsFlow = MutableStateFlow(initialRecords)

    override fun getIncomes(): Flow<List<IncomeRecord>> = recordsFlow

    override suspend fun addIncome(record: IncomeRecord): Long = record.id

    override suspend fun deleteIncome(id: Long) = Unit
}
