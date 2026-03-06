package com.personal.lifeOS.features.assistant.data.datasource

import com.personal.lifeOS.features.assistant.data.repository.AssistantRepositoryImpl
import com.personal.lifeOS.features.assistant.domain.repository.AssistantRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AssistantModule {

    @Binds
    @Singleton
    abstract fun bindAssistantRepository(
        impl: AssistantRepositoryImpl
    ): AssistantRepository
}
