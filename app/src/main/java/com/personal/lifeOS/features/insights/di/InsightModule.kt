package com.personal.lifeOS.features.insights.di

import com.personal.lifeOS.features.insights.data.repository.InsightRepositoryImpl
import com.personal.lifeOS.features.insights.domain.repository.InsightRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class InsightModule {
    @Binds
    @Singleton
    abstract fun bindInsightRepository(impl: InsightRepositoryImpl): InsightRepository
}
