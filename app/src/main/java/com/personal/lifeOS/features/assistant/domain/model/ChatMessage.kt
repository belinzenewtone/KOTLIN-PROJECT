package com.personal.lifeOS.features.assistant.domain.model

data class ChatMessage(
    val id: Long = System.currentTimeMillis(),
    val content: String,
    val sender: MessageSender,
    val timestamp: Long = System.currentTimeMillis(),
    val isTyping: Boolean = false,
)

enum class MessageSender {
    USER,
    ASSISTANT,
}

data class Insight(
    val title: String,
    val description: String,
    val type: InsightType,
    val value: String? = null,
)

enum class InsightType {
    SPENDING_ALERT,
    BUDGET_TIP,
    PRODUCTIVITY,
    SCHEDULE,
    TREND,
}

// Suggested prompts shown to user
val suggestedPrompts =
    listOf(
        "How much did I spend today?",
        "What's my biggest expense this month?",
        "Show my spending by category",
        "How many tasks are pending?",
        "What events do I have this week?",
        "Am I spending more than last week?",
    )
