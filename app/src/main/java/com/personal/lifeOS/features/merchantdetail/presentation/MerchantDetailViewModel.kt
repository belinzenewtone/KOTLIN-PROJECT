package com.personal.lifeOS.features.merchantdetail.presentation

import androidx.lifecycle.SavedStateHandle
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
import java.net.URLDecoder
import javax.inject.Inject

data class MerchantDetailUiState(
    val merchantName: String = "",
    val transactions: List<TransactionEntity> = emptyList(),
    val totalSpend: Double = 0.0,
    val transactionCount: Int = 0,
    val averageAmount: Double = 0.0,
    val isLoading: Boolean = true,
    val error: String? = null,
)

@HiltViewModel
class MerchantDetailViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val transactionDao: TransactionDao,
        private val authSessionStore: AuthSessionStore,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(MerchantDetailUiState())
        val uiState: StateFlow<MerchantDetailUiState> = _uiState.asStateFlow()

        init {
            val encodedMerchant = savedStateHandle.get<String>("merchant") ?: ""
            val merchant = try {
                URLDecoder.decode(encodedMerchant, "UTF-8")
            } catch (_: Exception) {
                encodedMerchant
            }
            _uiState.update { it.copy(merchantName = merchant) }
            observeMerchantTransactions(merchant)
        }

        private fun observeMerchantTransactions(merchant: String) {
            viewModelScope.launch {
                val userId = authSessionStore.getUserId()
                transactionDao.getTransactionsByMerchant(userId, merchant)
                    .onEach { txs ->
                        val outflows = txs.filter {
                            it.transactionType.uppercase() !in setOf("RECEIVED", "DEPOSIT")
                        }
                        val total = outflows.sumOf { it.amount }
                        val avg = if (outflows.isNotEmpty()) total / outflows.size else 0.0
                        _uiState.update {
                            it.copy(
                                transactions = txs,
                                totalSpend = total,
                                transactionCount = outflows.size,
                                averageAmount = avg,
                                isLoading = false,
                            )
                        }
                    }
                    .catch { e -> _uiState.update { it.copy(error = e.message, isLoading = false) } }
                    .launchIn(viewModelScope)
            }
        }
    }
