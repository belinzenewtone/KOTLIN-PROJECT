package com.personal.lifeOS.features.assistant.domain.model

data class CreateTaskActionPayload(
    val title: String,
    val description: String = "",
    val dueAt: Long? = null,
)

data class LogExpenseActionPayload(
    val amount: Double,
    val merchant: String,
    val category: String = "General",
    val date: Long = System.currentTimeMillis(),
)
