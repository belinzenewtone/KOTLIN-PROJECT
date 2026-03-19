package com.personal.lifeOS.core.di

import com.personal.lifeOS.platform.sms.ingestion.DefaultMpesaIngestionPipeline
import com.personal.lifeOS.platform.sms.ingestion.MpesaIngestionPipeline
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class PlatformSmsModule {
    @Binds
    @Singleton
    abstract fun bindMpesaIngestionPipeline(impl: DefaultMpesaIngestionPipeline): MpesaIngestionPipeline
}
