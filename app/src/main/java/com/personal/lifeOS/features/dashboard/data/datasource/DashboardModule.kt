package com.personal.lifeOS.features.dashboard.data.datasource

import com.personal.lifeOS.features.dashboard.data.repository.DashboardRepositoryImpl
import com.personal.lifeOS.features.dashboard.domain.repository.DashboardRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DashboardModule {
    @Binds
    @Singleton
    abstract fun bindDashboardRepository(impl: DashboardRepositoryImpl): DashboardRepository
}
