package com.personal.lifeOS.feature.finance.data.repository

import androidx.paging.PagingData
import com.personal.lifeOS.feature.finance.domain.repository.FinanceRepository
import com.personal.lifeOS.features.budget.domain.model.Budget
import com.personal.lifeOS.features.budget.domain.model.BudgetPeriod
import com.personal.lifeOS.features.budget.domain.repository.BudgetRepository
import com.personal.lifeOS.features.expenses.domain.model.CategoryBreakdown
import com.personal.lifeOS.features.expenses.domain.model.Transaction
import com.personal.lifeOS.features.expenses.domain.repository.ExpenseRepository
import com.personal.lifeOS.features.income.domain.model.IncomeRecord
import com.personal.lifeOS.features.income.domain.repository.IncomeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FinanceRepositoryImplTest {
    @Test
    fun `observe snapshot combines ledger budget and income streams`() =
        runTest {
            val expenseRepository = FakeExpenseRepository()
            val budgetRepository = FakeBudgetRepository()
            val incomeRepository = FakeIncomeRepository()
            val repository: FinanceRepository =
                FinanceRepositoryImpl(
                    expenseRepository = expenseRepository,
                    budgetRepository = budgetRepository,
                    incomeRepository = incomeRepository,
                )
            val snapshots = repository.observeSnapshot()

            val initial = snapshots.first()
            assertTrue(initial.transactions.isEmpty())
            assertTrue(initial.budgets.isEmpty())
            assertTrue(initial.incomes.isEmpty())
            assertEquals(0.0, initial.totalExpenses, 0.0)
            assertEquals(0.0, initial.totalIncome, 0.0)

            expenseRepository.transactions.value =
                listOf(
                    Transaction(
                        id = 1L,
                        amount = 1200.0,
                        merchant = "Naivas",
                        category = "Groceries",
                        date = 1_700_100_000_000L,
                    ),
                    Transaction(
                        id = 2L,
                        amount = 800.0,
                        merchant = "Fuel Station",
                        category = "Transport",
                        date = 1_700_200_000_000L,
                    ),
                )
            budgetRepository.budgets.value =
                listOf(
                    Budget(
                        id = 10L,
                        category = "GROCERIES",
                        limitAmount = 2_000.0,
                        period = BudgetPeriod.MONTHLY,
                    ),
                )
            incomeRepository.incomes.value =
                listOf(
                    IncomeRecord(
                        id = 20L,
                        amount = 4_500.0,
                        source = "Salary",
                        date = 1_700_000_000_000L,
                    ),
                )

            val updated =
                snapshots.first { snapshot ->
                    snapshot.transactions.size == 2 &&
                        snapshot.budgets.size == 1 &&
                        snapshot.incomes.size == 1
                }
            assertEquals(2, updated.transactions.size)
            assertEquals(1, updated.budgets.size)
            assertEquals(1, updated.incomes.size)
            assertEquals(2_000.0, updated.totalExpenses, 0.0)
            assertEquals(4_500.0, updated.totalIncome, 0.0)
            assertEquals(2_500.0, updated.netBalance, 0.0)
            assertEquals(2_000.0, updated.totalBudgetLimit, 0.0)
            assertEquals(100f, updated.budgetPressurePercent, 0.01f)
        }
}

private class FakeExpenseRepository : ExpenseRepository {
    val transactions = MutableStateFlow<List<Transaction>>(emptyList())

    override fun getAllTransactions(): Flow<List<Transaction>> = transactions

    override fun getTransactionsBetween(
        start: Long,
        end: Long,
    ): Flow<List<Transaction>> = transactions

    override fun getByCategory(category: String): Flow<List<Transaction>> = transactions

    override fun getTotalSpendingBetween(
        start: Long,
        end: Long,
    ): Flow<Double> = MutableStateFlow(0.0)

    override fun getCategoryBreakdown(
        start: Long,
        end: Long,
    ): Flow<List<CategoryBreakdown>> = MutableStateFlow(emptyList())

    override fun getTransactionCount(): Flow<Int> = MutableStateFlow(0)

    override suspend fun addTransaction(transaction: Transaction): Long = transaction.id

    override suspend fun updateTransaction(transaction: Transaction) = Unit

    override suspend fun deleteTransaction(transaction: Transaction) = Unit

    override suspend fun getById(id: Long): Transaction? = transactions.value.firstOrNull { it.id == id }

    override suspend fun existsByMpesaCode(code: String): Boolean = false

    override suspend fun existsBySourceHash(sourceHash: String): Boolean = false

    override suspend fun existsBySemanticHash(semanticHash: String): Boolean = false

    override suspend fun existsPotentialDuplicate(
        amount: Double,
        merchant: String,
        date: Long,
        windowMillis: Long,
    ): Boolean = false

    override suspend fun importFromSms(smsBody: String): Transaction? = null

    override suspend fun updateMerchantCategory(
        merchant: String,
        category: String,
    ) = Unit

    override fun pagedTransactions(
        startMs: Long?,
        endMs: Long?,
        searchQuery: String,
    ): Flow<PagingData<Transaction>> = flowOf(PagingData.empty())
}

private class FakeBudgetRepository : BudgetRepository {
    val budgets = MutableStateFlow<List<Budget>>(emptyList())

    override fun getBudgets(): Flow<List<Budget>> = budgets

    override suspend fun addBudget(budget: Budget): Long = budget.id

    override suspend fun updateBudget(budget: Budget) = Unit

    override suspend fun deleteBudget(id: Long) = Unit
}

private class FakeIncomeRepository : IncomeRepository {
    val incomes = MutableStateFlow<List<IncomeRecord>>(emptyList())

    override fun getIncomes(): Flow<List<IncomeRecord>> = incomes

    override suspend fun addIncome(record: IncomeRecord): Long = record.id

    override suspend fun deleteIncome(id: Long) = Unit
}
