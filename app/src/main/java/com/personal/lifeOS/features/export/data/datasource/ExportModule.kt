package com.personal.lifeOS.features.export.data.datasource

import com.personal.lifeOS.features.export.data.repository.ExportRepositoryImpl
import com.personal.lifeOS.features.export.domain.repository.ExportRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ExportModule {
    @Binds
    @Singleton
    abstract fun bindExportRepository(impl: ExportRepositoryImpl): ExportRepository
}
