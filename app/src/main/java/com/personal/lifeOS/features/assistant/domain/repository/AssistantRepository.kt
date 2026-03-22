package com.personal.lifeOS.features.assistant.domain.repository

import com.personal.lifeOS.features.assistant.domain.model.ChatMessage

interface AssistantRepository {
    suspend fun processMessage(userMessage: String): ChatMessage

    suspend fun loadConversationHistory(): List<ChatMessage>

    suspend fun saveMessage(
        message: ChatMessage,
        actionPayload: String? = null,
        isPreview: Boolean = false,
    )

    suspend fun clearConversationHistory()
}
