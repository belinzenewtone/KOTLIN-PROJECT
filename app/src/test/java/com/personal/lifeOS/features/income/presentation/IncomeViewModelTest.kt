package com.personal.lifeOS.features.income.presentation

import com.personal.lifeOS.features.income.domain.model.IncomeRecord
import com.personal.lifeOS.features.income.domain.repository.IncomeRepository
import com.personal.lifeOS.features.income.domain.usecase.AddIncomeUseCase
import com.personal.lifeOS.features.income.domain.usecase.DeleteIncomeUseCase
import com.personal.lifeOS.features.income.domain.usecase.ObserveIncomeSnapshotUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
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
class IncomeViewModelTest {
    @Test
    fun `save income validates required fields`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            try {
                val repository = IncomeVmFakeRepository()
                val viewModel = createViewModel(repository)
                advanceUntilIdle()

                viewModel.showAddDialog()
                viewModel.saveIncome()
                advanceUntilIdle()

                assertEquals("Source is required", viewModel.uiState.value.error)
                assertTrue(repository.savedRecords.isEmpty())
            } finally {
                Dispatchers.resetMain()
            }
        }

    @Test
    fun `save income writes via use case and closes dialog`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            try {
                val repository = IncomeVmFakeRepository()
                val viewModel = createViewModel(repository)
                advanceUntilIdle()

                viewModel.showAddDialog()
                viewModel.setSource("Salary")
                viewModel.setAmount("95000")
                viewModel.setNote("March payroll")
                viewModel.saveIncome()
                advanceUntilIdle()

                assertFalse(viewModel.uiState.value.showDialog)
                assertEquals(1, repository.savedRecords.size)
                assertEquals("Salary", repository.savedRecords.first().source)
                assertEquals(95_000.0, repository.savedRecords.first().amount, 0.0)
                assertFalse(viewModel.uiState.value.isLoading)
                assertEquals(95_000.0, viewModel.uiState.value.monthTotal, 0.0)
            } finally {
                Dispatchers.resetMain()
            }
        }
}

private fun createViewModel(repository: IncomeVmFakeRepository): IncomeViewModel {
    return IncomeViewModel(
        observeIncomeSnapshotUseCase = ObserveIncomeSnapshotUseCase(repository),
        addIncomeUseCase = AddIncomeUseCase(repository),
        deleteIncomeUseCase = DeleteIncomeUseCase(repository),
    )
}

private class IncomeVmFakeRepository : IncomeRepository {
    val savedRecords = mutableListOf<IncomeRecord>()
    private val recordsFlow = MutableStateFlow<List<IncomeRecord>>(emptyList())

    override fun getIncomes(): Flow<List<IncomeRecord>> = recordsFlow

    override suspend fun addIncome(record: IncomeRecord): Long {
        val nextId = (savedRecords.size + 1).toLong()
        val persisted = record.copy(id = nextId)
        savedRecords += persisted
        recordsFlow.value = savedRecords.toList()
        return nextId
    }

    override suspend fun deleteIncome(id: Long) {
        savedRecords.removeAll { it.id == id }
        recordsFlow.value = savedRecords.toList()
    }
}
