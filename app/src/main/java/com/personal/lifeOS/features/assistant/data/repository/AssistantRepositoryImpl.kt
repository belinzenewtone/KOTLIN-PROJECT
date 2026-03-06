package com.personal.lifeOS.features.assistant.data.repository

import com.personal.lifeOS.features.assistant.data.datasource.LocalAIEngine
import com.personal.lifeOS.features.assistant.domain.model.ChatMessage
import com.personal.lifeOS.features.assistant.domain.model.MessageSender
import com.personal.lifeOS.features.assistant.domain.repository.AssistantRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AssistantRepositoryImpl @Inject constructor(
    private val localAIEngine: LocalAIEngine
) : AssistantRepository {

    override suspend fun processMessage(userMessage: String): ChatMessage {
        val response = localAIEngine.processQuery(userMessage)
        return ChatMessage(
            content = response,
            sender = MessageSender.ASSISTANT
        )
    }
}
