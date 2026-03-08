package com.personal.lifeOS.features.assistant.data.repository

import com.personal.lifeOS.core.utils.ApiConfig
import com.personal.lifeOS.features.assistant.data.datasource.DataContextBuilder
import com.personal.lifeOS.features.assistant.data.datasource.LocalAIEngine
import com.personal.lifeOS.features.assistant.data.datasource.OpenAIClient
import com.personal.lifeOS.features.assistant.domain.model.ChatMessage
import com.personal.lifeOS.features.assistant.domain.model.MessageSender
import com.personal.lifeOS.features.assistant.domain.repository.AssistantRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Routes queries to the assistant proxy if configured, otherwise falls back to local AI engine.
 *
 * Flow:
 * 1. Check if assistant proxy endpoint is configured
 * 2. If yes: build data context -> call proxy -> return response
 * 3. If no (or if proxy fails): use local rule-based engine
 */
@Singleton
class AssistantRepositoryImpl
    @Inject
    constructor(
        private val localAIEngine: LocalAIEngine,
        private val openAIClient: OpenAIClient,
        private val contextBuilder: DataContextBuilder,
    ) : AssistantRepository {
        override suspend fun processMessage(userMessage: String): ChatMessage {
            // Try assistant proxy first if configured
            if (ApiConfig.isAssistantProxyConfigured()) {
                try {
                    val context = contextBuilder.buildContext()
                    val aiResponse = openAIClient.chat(userMessage, context)

                    if (aiResponse != null) {
                        return ChatMessage(
                            content = aiResponse,
                            sender = MessageSender.ASSISTANT,
                        )
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            // Fallback to local rule-based engine
            return try {
                val response = localAIEngine.processQuery(userMessage)
                ChatMessage(content = response, sender = MessageSender.ASSISTANT)
            } catch (e: Exception) {
                e.printStackTrace()
                ChatMessage(
                    content = "Sorry, I encountered an error. Please try again.",
                    sender = MessageSender.ASSISTANT,
                )
            }
        }
    }
