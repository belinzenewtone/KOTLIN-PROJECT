package com.personal.lifeOS.features.assistant.domain.repository

import com.personal.lifeOS.features.assistant.domain.model.AssistantActionCommitResult
import com.personal.lifeOS.features.assistant.domain.model.AssistantActionProposal

interface AssistantActionExecutor {
    suspend fun commit(proposal: AssistantActionProposal): AssistantActionCommitResult
}
