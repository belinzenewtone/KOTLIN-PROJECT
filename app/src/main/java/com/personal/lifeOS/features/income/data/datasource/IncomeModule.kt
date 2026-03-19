package com.personal.lifeOS.features.income.data.datasource

import com.personal.lifeOS.features.income.data.repository.IncomeRepositoryImpl
import com.personal.lifeOS.features.income.domain.repository.IncomeRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class IncomeModule {
    @Binds
    @Singleton
    abstract fun bindIncomeRepository(impl: IncomeRepositoryImpl): IncomeRepository
}
