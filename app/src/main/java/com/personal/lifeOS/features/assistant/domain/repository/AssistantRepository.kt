package com.personal.lifeOS.features.assistant.domain.repository

import com.personal.lifeOS.features.assistant.domain.model.ChatMessage

interface AssistantRepository {
    suspend fun processMessage(userMessage: String): ChatMessage
}
