package com.personal.lifeOS.features.assistant.domain.model

enum class AssistantActionType {
    CREATE_TASK,
    RESCHEDULE_TASK,
    CREATE_EVENT,
    LOG_EXPENSE,
    CREATE_BUDGET,
    EXPLAIN_INSIGHT,
    REVIEW_SPENDING,
    OPEN_REVIEW,
    OPEN_SEARCH_RESULT,
    FIX_IMPORT_ISSUE,
}

data class AssistantActionPreview(
    val title: String,
    val summary: String,
    val riskLabel: String = "Safe",
)

data class AssistantActionProposal(
    val id: String,
    val type: AssistantActionType,
    val preview: AssistantActionPreview,
    val payload: String,
)

sealed interface AssistantActionCommitResult {
    data object Success : AssistantActionCommitResult

    data class Error(val message: String) : AssistantActionCommitResult
}
