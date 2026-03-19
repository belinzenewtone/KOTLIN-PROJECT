package com.personal.lifeOS.feature.finance.di

import com.personal.lifeOS.feature.finance.data.repository.FinanceRepositoryImpl
import com.personal.lifeOS.feature.finance.domain.repository.FinanceRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class FinanceModule {
    @Binds
    @Singleton
    abstract fun bindFinanceRepository(impl: FinanceRepositoryImpl): FinanceRepository
}
