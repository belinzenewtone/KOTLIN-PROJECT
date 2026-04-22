package com.personal.lifeOS.features.categorize.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personal.lifeOS.core.database.LocalIdGenerator
import com.personal.lifeOS.core.database.dao.MerchantCategoryDao
import com.personal.lifeOS.core.database.dao.TransactionDao
import com.personal.lifeOS.core.database.entity.MerchantCategoryEntity
import com.personal.lifeOS.core.database.entity.TransactionEntity
import com.personal.lifeOS.core.security.AuthSessionStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * A single group shown in the categorize wizard — all uncategorized transactions
 * from the same merchant collapsed into one assignable card.
 */
data class MerchantGroup(
    val merchant: String,
    val transactionCount: Int,
    val totalAmount: Double,
    val latestDate: Long,
    /** One representative row, used for display context (amount, date). */
    val sample: TransactionEntity,
)

data class CategorizeUiState(
    val groups: List<MerchantGroup> = emptyList(),
    val totalTransactionCount: Int = 0,
    val isLoading: Boolean = true,
    val successMessage: String? = null,
    val error: String? = null,
)

@HiltViewModel
class CategorizeViewModel
    @Inject
    constructor(
        private val transactionDao: TransactionDao,
        private val merchantCategoryDao: MerchantCategoryDao,
        private val authSessionStore: AuthSessionStore,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(CategorizeUiState())
        val uiState: StateFlow<CategorizeUiState> = _uiState.asStateFlow()

        // Resolved once; safe because AuthSessionStore is a singleton and userId
        // does not change while the screen is open.
        private val userId: String = authSessionStore.getUserId()

        init {
            observeUncategorized()
        }

        private fun observeUncategorized() {
            transactionDao.getUncategorizedTransactions(userId)
                .map { txs -> txs.toMerchantGroups() }
                .onEach { groups ->
                    _uiState.update {
                        it.copy(
                            groups = groups,
                            totalTransactionCount = groups.sumOf { g -> g.transactionCount },
                            isLoading = false,
                        )
                    }
                }
                .catch { e -> _uiState.update { it.copy(error = e.message, isLoading = false) } }
                .launchIn(viewModelScope)
        }

        /**
         * Assign [newCategory] to **all** uncategorized transactions from [merchant] in one
         * SQL UPDATE, then save a user-corrected merchant mapping so future M-Pesa imports
         * from the same merchant auto-categorize without appearing here again.
         *
         * Room's reactive query re-emits automatically after the UPDATE, collapsing the
         * group list — no manual state removal needed.
         */
        fun assignCategoryToMerchant(merchant: String, newCategory: String) {
            viewModelScope.launch {
                try {
                    val now = System.currentTimeMillis()

                    // 1 — Batch-update all uncategorized rows for this merchant
                    transactionDao.updateCategoryForMerchant(
                        userId = userId,
                        merchant = merchant,
                        category = newCategory,
                        updatedAt = now,
                    )

                    // 2 — Persist merchant → category mapping so future imports skip this screen
                    val existing = merchantCategoryDao.getByMerchant(merchant.uppercase(), userId)
                    val stableId = existing?.id ?: LocalIdGenerator.nextId()
                    merchantCategoryDao.insert(
                        MerchantCategoryEntity(
                            id = stableId,
                            merchant = merchant.uppercase(),
                            category = newCategory,
                            confidence = 1.0f,
                            userCorrected = true,
                            userId = userId,
                        ),
                    )

                    _uiState.update { it.copy(successMessage = "Saved for $merchant") }
                } catch (e: Exception) {
                    _uiState.update { it.copy(error = e.message) }
                }
            }
        }

        fun clearMessages() {
            _uiState.update { it.copy(successMessage = null, error = null) }
        }

        // ── Helpers ────────────────────────────────────────────────────────────

        private fun List<TransactionEntity>.toMerchantGroups(): List<MerchantGroup> =
            groupBy { it.merchant }
                .map { (merchant, txs) ->
                    MerchantGroup(
                        merchant = merchant,
                        transactionCount = txs.size,
                        totalAmount = txs.sumOf { it.amount },
                        latestDate = txs.maxOf { it.date },
                        sample = txs.first(),
                    )
                }
                .sortedByDescending { it.latestDate }
    }
