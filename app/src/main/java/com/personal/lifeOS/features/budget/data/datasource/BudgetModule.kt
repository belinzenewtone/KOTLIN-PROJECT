package com.personal.lifeOS.features.budget.data.datasource

import com.personal.lifeOS.features.budget.data.repository.BudgetRepositoryImpl
import com.personal.lifeOS.features.budget.domain.repository.BudgetRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class BudgetModule {
    @Binds
    @Singleton
    abstract fun bindBudgetRepository(impl: BudgetRepositoryImpl): BudgetRepository
}
