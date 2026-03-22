package com.personal.lifeOS.features.expenses.data.datasource

import com.personal.lifeOS.features.expenses.data.repository.ExpenseRepositoryImpl
import com.personal.lifeOS.features.expenses.data.repository.FulizaLoanRepositoryImpl
import com.personal.lifeOS.features.expenses.domain.repository.ExpenseRepository
import com.personal.lifeOS.features.expenses.domain.repository.FulizaLoanRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ExpenseModule {
    @Binds
    @Singleton
    abstract fun bindExpenseRepository(impl: ExpenseRepositoryImpl): ExpenseRepository

    @Binds
    @Singleton
    abstract fun bindFulizaLoanRepository(impl: FulizaLoanRepositoryImpl): FulizaLoanRepository
}
