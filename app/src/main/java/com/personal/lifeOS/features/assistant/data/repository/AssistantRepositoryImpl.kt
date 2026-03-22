package com.personal.lifeOS.features.assistant.data.repository

import com.personal.lifeOS.core.database.LocalIdGenerator
import com.personal.lifeOS.core.database.dao.AssistantConversationDao
import com.personal.lifeOS.core.database.dao.AssistantMessageDao
import com.personal.lifeOS.core.database.entity.AssistantConversationEntity
import com.personal.lifeOS.core.database.entity.AssistantMessageEntity
import com.personal.lifeOS.core.security.AuthSessionStore
import com.personal.lifeOS.core.utils.ApiConfig
import com.personal.lifeOS.features.assistant.data.datasource.AssistantProxyClient
import com.personal.lifeOS.features.assistant.data.datasource.DataContextBuilder
import com.personal.lifeOS.features.assistant.data.datasource.LocalAIEngine
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
        private val assistantProxyClient: AssistantProxyClient,
        private val contextBuilder: DataContextBuilder,
        private val authSessionStore: AuthSessionStore,
        private val conversationDao: AssistantConversationDao,
        private val messageDao: AssistantMessageDao,
    ) : AssistantRepository {
        override suspend fun processMessage(userMessage: String): ChatMessage {
            // Try assistant proxy first if configured
            if (ApiConfig.isAssistantProxyConfigured()) {
                try {
                    val context = contextBuilder.buildContext()
                    val aiResponse = assistantProxyClient.chat(userMessage, context)

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

        override suspend fun loadConversationHistory(): List<ChatMessage> {
            val userId = activeUserId()
            val latestConversation = conversationDao.getLatestForUser(userId) ?: return emptyList()
            return messageDao.getConversationMessages(
                userId = userId,
                conversationId = latestConversation.id,
            ).map { entity ->
                entity.toChatMessage()
            }
        }

        override suspend fun saveMessage(
            message: ChatMessage,
            actionPayload: String?,
            isPreview: Boolean,
        ) {
            val userId = activeUserId()
            val conversationId =
                ensureConversation(
                    userId = userId,
                    initialTitle = if (message.sender == MessageSender.USER) message.content else null,
                )
            val now = System.currentTimeMillis()
            val stableMessageId = if (message.id > 0L) message.id else LocalIdGenerator.nextId()

            messageDao.insert(
                AssistantMessageEntity(
                    id = stableMessageId,
                    userId = userId,
                    conversationId = conversationId,
                    role = message.sender.toRole(),
                    content = message.content,
                    actionPayload = actionPayload,
                    isPreview = isPreview,
                    createdAt = message.timestamp,
                    updatedAt = now,
                ),
            )

            touchConversation(
                userId = userId,
                conversationId = conversationId,
                possibleTitle = if (message.sender == MessageSender.USER) message.content else null,
            )
        }

        private suspend fun ensureConversation(
            userId: String,
            initialTitle: String?,
        ): Long {
            val existing = conversationDao.getLatestForUser(userId)
            if (existing != null) return existing.id

            val id = LocalIdGenerator.nextId()
            conversationDao.insert(
                AssistantConversationEntity(
                    id = id,
                    userId = userId,
                    title = initialTitle.toConversationTitle(),
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis(),
                ),
            )
            return id
        }

        private suspend fun touchConversation(
            userId: String,
            conversationId: Long,
            possibleTitle: String?,
        ) {
            val conversation = conversationDao.getById(userId, conversationId) ?: return
            val nextTitle =
                if (conversation.title == "Conversation" && !possibleTitle.isNullOrBlank()) {
                    possibleTitle.toConversationTitle()
                } else {
                    conversation.title
                }
            conversationDao.update(
                conversation.copy(
                    title = nextTitle,
                    updatedAt = System.currentTimeMillis(),
                    revision = conversation.revision + 1,
                ),
            )
        }

        override suspend fun clearConversationHistory() {
            val userId = activeUserId()
            messageDao.deleteAllForUser(userId)
            conversationDao.deleteAllForUser(userId)
        }

        private fun activeUserId(): String = authSessionStore.getUserId().ifBlank { "local" }

        private fun MessageSender.toRole(): String {
            return when (this) {
                MessageSender.USER -> "user"
                MessageSender.ASSISTANT -> "assistant"
            }
        }

        private fun AssistantMessageEntity.toChatMessage(): ChatMessage {
            return ChatMessage(
                id = id,
                content = content,
                sender = if (role.equals("user", ignoreCase = true)) MessageSender.USER else MessageSender.ASSISTANT,
                timestamp = createdAt,
            )
        }

        private fun String?.toConversationTitle(): String {
            if (this.isNullOrBlank()) return "Conversation"
            val oneLine = replace('\n', ' ').trim()
            return oneLine.take(60).ifBlank { "Conversation" }
        }
    }
