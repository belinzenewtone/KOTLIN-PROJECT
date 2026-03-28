package com.personal.lifeOS.features.budget.presentation

import androidx.paging.PagingData
import com.personal.lifeOS.features.budget.domain.model.Budget
import com.personal.lifeOS.features.budget.domain.model.BudgetPeriod
import com.personal.lifeOS.features.budget.domain.repository.BudgetRepository
import com.personal.lifeOS.features.budget.domain.usecase.AddBudgetUseCase
import com.personal.lifeOS.features.budget.domain.usecase.DeleteBudgetUseCase
import com.personal.lifeOS.features.budget.domain.usecase.ObserveBudgetProgressUseCase
import com.personal.lifeOS.features.expenses.domain.model.CategoryBreakdown
import com.personal.lifeOS.features.expenses.domain.model.Transaction
import com.personal.lifeOS.features.expenses.domain.repository.ExpenseRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BudgetViewModelTest {
    @Test
    fun `save budget validates required fields`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            try {
                val budgetRepository = BudgetVmFakeBudgetRepository()
                val viewModel = createViewModel(budgetRepository = budgetRepository)
                advanceUntilIdle()

                viewModel.showAddDialog()
                viewModel.saveBudget()
                advanceUntilIdle()

                assertEquals("Please select a category", viewModel.uiState.value.error)
                assertTrue(budgetRepository.savedBudgets.isEmpty())
            } finally {
                Dispatchers.resetMain()
            }
        }

    @Test
    fun `save budget writes via use case and closes dialog`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            try {
                val budgetRepository = BudgetVmFakeBudgetRepository()
                val viewModel = createViewModel(budgetRepository = budgetRepository)
                advanceUntilIdle()

                viewModel.showAddDialog()
                viewModel.setCategory("Food")
                viewModel.setLimit("1200")
                viewModel.setPeriod(BudgetPeriod.MONTHLY)
                viewModel.saveBudget()
                advanceUntilIdle()

                assertFalse(viewModel.uiState.value.showDialog)
                assertEquals(1, budgetRepository.savedBudgets.size)
                assertEquals("Food", budgetRepository.savedBudgets.first().category)
                assertEquals(1200.0, budgetRepository.savedBudgets.first().limitAmount, 0.0)
            } finally {
                Dispatchers.resetMain()
            }
        }
}

private fun createViewModel(
    budgetRepository: BudgetVmFakeBudgetRepository,
): BudgetViewModel {
    val expenseRepository = BudgetVmFakeExpenseRepository()
    return BudgetViewModel(
        observeBudgetProgressUseCase =
            ObserveBudgetProgressUseCase(
                budgetRepository = budgetRepository,
                expenseRepository = expenseRepository,
            ),
        addBudgetUseCase = AddBudgetUseCase(budgetRepository),
        deleteBudgetUseCase = DeleteBudgetUseCase(budgetRepository),
        budgetRepository = budgetRepository,
    )
}

private class BudgetVmFakeBudgetRepository : BudgetRepository {
    val savedBudgets = mutableListOf<Budget>()
    private val budgetsFlow = MutableStateFlow<List<Budget>>(emptyList())

    override fun getBudgets(): Flow<List<Budget>> = budgetsFlow

    override suspend fun addBudget(budget: Budget): Long {
        val nextId = (savedBudgets.size + 1).toLong()
        val persisted = budget.copy(id = nextId)
        savedBudgets += persisted
        budgetsFlow.value = savedBudgets.toList()
        return nextId
    }

    override suspend fun updateBudget(budget: Budget) = Unit

    override suspend fun deleteBudget(id: Long) {
        savedBudgets.removeAll { it.id == id }
        budgetsFlow.value = savedBudgets.toList()
    }
}

private class BudgetVmFakeExpenseRepository : ExpenseRepository {
    private val transactions = MutableStateFlow<List<Transaction>>(emptyList())

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
