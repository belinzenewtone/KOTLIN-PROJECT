package com.personal.lifeOS.features.assistant.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personal.lifeOS.core.datastore.FeatureFlag
import com.personal.lifeOS.core.datastore.FeatureFlagStore
import com.personal.lifeOS.features.assistant.domain.model.AssistantActionCommitResult
import com.personal.lifeOS.features.assistant.domain.model.AssistantActionProposal
import com.personal.lifeOS.features.assistant.domain.model.ChatMessage
import com.personal.lifeOS.features.assistant.domain.model.MessageSender
import com.personal.lifeOS.features.assistant.domain.repository.AssistantActionExecutor
import com.personal.lifeOS.features.assistant.domain.repository.AssistantRepository
import com.personal.lifeOS.features.assistant.domain.usecase.BuildAssistantActionProposalUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AssistantUiState(
    val messages: List<ChatMessage> = emptyList(),
    val inputText: String = "",
    val isProcessing: Boolean = false,
    val pendingProposal: AssistantActionProposal? = null,
    val proposalResultMessage: String? = null,
)

@HiltViewModel
class AssistantViewModel
    @Inject
    constructor(
        private val repository: AssistantRepository,
        private val actionExecutor: AssistantActionExecutor,
        private val buildActionProposalUseCase: BuildAssistantActionProposalUseCase,
        private val featureFlagStore: FeatureFlagStore,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(AssistantUiState())
        val uiState: StateFlow<AssistantUiState> = _uiState.asStateFlow()

        init {
            viewModelScope.launch {
                val history = runCatching { repository.loadConversationHistory() }.getOrDefault(emptyList())
                if (history.isNotEmpty()) {
                    _uiState.update { it.copy(messages = history) }
                    return@launch
                }

                val welcomeMessage = welcomeMessage()
                _uiState.update { it.copy(messages = listOf(welcomeMessage)) }
                persistMessageSafely(welcomeMessage)
            }
        }

        fun onEvent(event: AssistantUiEvent) {
            when (event) {
                is AssistantUiEvent.InputChanged -> updateInput(event.text)
                AssistantUiEvent.SendMessage -> sendMessage()
                AssistantUiEvent.ApproveProposal -> approvePendingProposal()
                AssistantUiEvent.RejectProposal -> rejectPendingProposal()
            }
        }

        fun updateInput(text: String) {
            _uiState.update { it.copy(inputText = text) }
        }

        fun sendMessage() {
            val text = _uiState.value.inputText.trim()
            if (text.isEmpty()) return

            val userMessage = ChatMessage(content = text, sender = MessageSender.USER)

            _uiState.update {
                it.copy(
                    messages = it.messages + userMessage,
                    inputText = "",
                    isProcessing = true,
                )
            }

            viewModelScope.launch {
                try {
                    persistMessageSafely(userMessage)
                    delay(300)
                    val actionsEnabled =
                        runCatching { featureFlagStore.isEnabled(FeatureFlag.ASSISTANT_ACTIONS) }
                            .getOrDefault(true)
                    val proposal =
                        if (actionsEnabled) {
                            buildActionProposalUseCase(text)
                        } else {
                            null
                        }
                    if (proposal != null) {
                        val proposalMessage =
                            ChatMessage(
                                content = "I drafted an action proposal. Please review before committing changes.",
                                sender = MessageSender.ASSISTANT,
                            )
                        _uiState.update {
                            it.copy(
                                messages = it.messages + proposalMessage,
                                pendingProposal = proposal,
                                proposalResultMessage = null,
                                isProcessing = false,
                            )
                        }
                        persistMessageSafely(
                            message = proposalMessage,
                            actionPayload = proposal.payload,
                            isPreview = true,
                        )
                        return@launch
                    }

                    val response = repository.processMessage(text)
                    _uiState.update {
                        it.copy(
                            messages = it.messages + response,
                            isProcessing = false,
                        )
                    }
                    persistMessageSafely(response)
                } catch (e: Exception) {
                    val errorMessage =
                        ChatMessage(
                            content = "Sorry, I ran into an error. Please try again.",
                            sender = MessageSender.ASSISTANT,
                        )
                    _uiState.update {
                        it.copy(
                            messages = it.messages + errorMessage,
                            isProcessing = false,
                        )
                    }
                    persistMessageSafely(errorMessage)
                }
            }
        }

        fun approvePendingProposal() {
            val proposal = _uiState.value.pendingProposal ?: return
            _uiState.update { it.copy(isProcessing = true, proposalResultMessage = null) }

            viewModelScope.launch {
                when (val result = actionExecutor.commit(proposal)) {
                    AssistantActionCommitResult.Success -> {
                        val successMessage =
                            ChatMessage(
                                content = "Action applied: ${proposal.preview.summary}",
                                sender = MessageSender.ASSISTANT,
                            )
                        _uiState.update {
                            it.copy(
                                messages = it.messages + successMessage,
                                pendingProposal = null,
                                proposalResultMessage = "Committed successfully",
                                isProcessing = false,
                            )
                        }
                        persistMessageSafely(successMessage)
                    }

                    is AssistantActionCommitResult.Error -> {
                        val errorMessage =
                            ChatMessage(
                                content = "Action failed: ${result.message}",
                                sender = MessageSender.ASSISTANT,
                            )
                        _uiState.update {
                            it.copy(
                                messages = it.messages + errorMessage,
                                proposalResultMessage = result.message,
                                isProcessing = false,
                            )
                        }
                        persistMessageSafely(errorMessage)
                    }
                }
            }
        }

        fun rejectPendingProposal() {
            val proposal = _uiState.value.pendingProposal ?: return
            val cancelledMessage =
                ChatMessage(
                    content = "Action cancelled: ${proposal.preview.summary}",
                    sender = MessageSender.ASSISTANT,
                )
            _uiState.update {
                it.copy(
                    messages = it.messages + cancelledMessage,
                    pendingProposal = null,
                    proposalResultMessage = "Cancelled",
                )
            }
            viewModelScope.launch { persistMessageSafely(cancelledMessage) }
        }

        fun clearProposalResultMessage() {
            _uiState.update { it.copy(proposalResultMessage = null) }
        }

        fun sendSuggestedPrompt(prompt: String) {
            _uiState.update { it.copy(inputText = prompt) }
            sendMessage()
        }

        private fun welcomeMessage(): ChatMessage {
            return ChatMessage(
                content = "Hey! I am your BELTECH assistant. Ask me about spending, tasks, or schedule.",
                sender = MessageSender.ASSISTANT,
            )
        }

        private suspend fun persistMessageSafely(
            message: ChatMessage,
            actionPayload: String? = null,
            isPreview: Boolean = false,
        ) {
            runCatching {
                repository.saveMessage(
                    message = message,
                    actionPayload = actionPayload,
                    isPreview = isPreview,
                )
            }
        }
    }
