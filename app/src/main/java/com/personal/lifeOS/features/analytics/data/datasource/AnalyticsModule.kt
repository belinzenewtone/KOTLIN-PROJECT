package com.personal.lifeOS.features.analytics.data.datasource

import com.personal.lifeOS.features.analytics.data.repository.AnalyticsRepositoryImpl
import com.personal.lifeOS.features.analytics.domain.repository.AnalyticsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AnalyticsModule {

    @Binds
    @Singleton
    abstract fun bindAnalyticsRepository(
        impl: AnalyticsRepositoryImpl
    ): AnalyticsRepository
}
