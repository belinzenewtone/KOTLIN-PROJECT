package com.personal.lifeOS.features.recurring.data.datasource

import com.personal.lifeOS.features.recurring.data.repository.RecurringRepositoryImpl
import com.personal.lifeOS.features.recurring.domain.repository.RecurringRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RecurringModule {
    @Binds
    @Singleton
    abstract fun bindRecurringRepository(impl: RecurringRepositoryImpl): RecurringRepository
}
