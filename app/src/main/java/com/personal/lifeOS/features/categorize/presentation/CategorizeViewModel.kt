package com.personal.lifeOS.features.categorize.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personal.lifeOS.core.database.dao.TransactionDao
import com.personal.lifeOS.core.database.entity.TransactionEntity
import com.personal.lifeOS.core.security.AuthSessionStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CategorizeUiState(
    val transactions: List<TransactionEntity> = emptyList(),
    val isLoading: Boolean = true,
    val successMessage: String? = null,
    val error: String? = null,
)

@HiltViewModel
class CategorizeViewModel
    @Inject
    constructor(
        private val transactionDao: TransactionDao,
        private val authSessionStore: AuthSessionStore,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(CategorizeUiState())
        val uiState: StateFlow<CategorizeUiState> = _uiState.asStateFlow()

        init {
            observeUncategorized()
        }

        private fun observeUncategorized() {
            viewModelScope.launch {
                val userId = authSessionStore.getUserId()
                transactionDao.getUncategorizedTransactions(userId)
                    .onEach { txs ->
                        _uiState.update { it.copy(transactions = txs, isLoading = false) }
                    }
                    .catch { e -> _uiState.update { it.copy(error = e.message, isLoading = false) } }
                    .launchIn(viewModelScope)
            }
        }

        fun assignCategory(transaction: TransactionEntity, newCategory: String) {
            viewModelScope.launch {
                try {
                    transactionDao.update(
                        transaction.copy(
                            category = newCategory,
                            updatedAt = System.currentTimeMillis(),
                        ),
                    )
                    _uiState.update { it.copy(successMessage = "Category updated") }
                } catch (e: Exception) {
                    _uiState.update { it.copy(error = e.message) }
                }
            }
        }

        fun clearMessages() {
            _uiState.update { it.copy(successMessage = null, error = null) }
        }
    }
