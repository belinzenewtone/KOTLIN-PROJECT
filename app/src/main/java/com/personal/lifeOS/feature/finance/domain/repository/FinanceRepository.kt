package com.personal.lifeOS.feature.finance.domain.repository

import androidx.paging.PagingData
import com.personal.lifeOS.feature.finance.domain.model.FinanceSnapshot
import com.personal.lifeOS.feature.finance.domain.model.FinanceTransaction
import com.personal.lifeOS.feature.finance.domain.model.FinanceTransactionFilter
import kotlinx.coroutines.flow.Flow

interface FinanceRepository {
    fun observeSnapshot(): Flow<FinanceSnapshot>

    fun pagedTransactions(
        filter: FinanceTransactionFilter,
        searchQuery: String = "",
    ): Flow<PagingData<FinanceTransaction>>
}
