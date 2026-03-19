package com.personal.lifeOS.features.assistant.presentation

import com.personal.lifeOS.core.ui.model.AssistantActionProposalUiModel
import com.personal.lifeOS.features.assistant.domain.model.AssistantActionProposal

sealed interface AssistantUiEvent {
    data class InputChanged(val text: String) : AssistantUiEvent

    data object SendMessage : AssistantUiEvent

    data object ApproveProposal : AssistantUiEvent

    data object RejectProposal : AssistantUiEvent
}

fun AssistantActionProposal.toUiModel(): AssistantActionProposalUiModel {
    return AssistantActionProposalUiModel(
        title = preview.title,
        summary = preview.summary,
        riskLabel = preview.riskLabel,
    )
}
