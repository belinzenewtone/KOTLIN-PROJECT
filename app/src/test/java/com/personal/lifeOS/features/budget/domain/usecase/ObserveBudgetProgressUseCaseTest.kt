package com.personal.lifeOS.features.budget.domain.usecase

import com.personal.lifeOS.features.budget.domain.model.Budget
import com.personal.lifeOS.features.budget.domain.model.BudgetPeriod
import com.personal.lifeOS.features.budget.domain.repository.BudgetRepository
import com.personal.lifeOS.features.expenses.domain.model.CategoryBreakdown
import com.personal.lifeOS.features.expenses.domain.model.Transaction
import com.personal.lifeOS.features.expenses.domain.repository.ExpenseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ObserveBudgetProgressUseCaseTest {
    @Test
    fun `maps monthly spending totals into matching budget categories`() =
        runTest {
            val budgetRepository =
                FakeBudgetRepository(
                    listOf(
                        Budget(
                            id = 1L,
                            category = "Groceries",
                            limitAmount = 5_000.0,
                            period = BudgetPeriod.MONTHLY,
                        ),
                        Budget(
                            id = 2L,
                            category = "Transport",
                            limitAmount = 2_000.0,
                            period = BudgetPeriod.MONTHLY,
                        ),
                    ),
                )
            val expenseRepository =
                FakeExpenseRepository(
                    listOf(
                        Transaction(
                            id = 10L,
                            amount = 1_200.0,
                            merchant = "Naivas",
                            category = "groceries",
                            date = 1_700_000_000_000L,
                        ),
                        Transaction(
                            id = 11L,
                            amount = 800.0,
                            merchant = "Quickmart",
                            category = " Groceries ",
                            date = 1_700_100_000_000L,
                        ),
                        Transaction(
                            id = 12L,
                            amount = 600.0,
                            merchant = "Rent",
                            category = "Housing",
                            date = 1_700_200_000_000L,
                        ),
                    ),
                )
            val useCase = ObserveBudgetProgressUseCase(budgetRepository, expenseRepository)

            val progress = useCase().first()

            assertEquals(2, progress.size)
            assertEquals(2_000.0, progress.first { it.budget.id == 1L }.spentAmount, 0.0)
            assertEquals(0.0, progress.first { it.budget.id == 2L }.spentAmount, 0.0)
        }
}

private class FakeBudgetRepository(
    initialBudgets: List<Budget>,
) : BudgetRepository {
    private val budgets = MutableStateFlow(initialBudgets)

    override fun getBudgets(): Flow<List<Budget>> = budgets

    override suspend fun addBudget(budget: Budget): Long = budget.id

    override suspend fun updateBudget(budget: Budget) = Unit

    override suspend fun deleteBudget(id: Long) = Unit
}

private class FakeExpenseRepository(
    initialTransactions: List<Transaction>,
) : ExpenseRepository {
    private val transactions = MutableStateFlow(initialTransactions)

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
}
