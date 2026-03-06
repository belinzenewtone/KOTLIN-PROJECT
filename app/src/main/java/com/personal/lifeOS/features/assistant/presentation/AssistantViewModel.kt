package com.personal.lifeOS.features.assistant.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personal.lifeOS.features.assistant.domain.model.ChatMessage
import com.personal.lifeOS.features.assistant.domain.model.MessageSender
import com.personal.lifeOS.features.assistant.domain.repository.AssistantRepository
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
    val isProcessing: Boolean = false
)

@HiltViewModel
class AssistantViewModel @Inject constructor(
    private val repository: AssistantRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AssistantUiState())
    val uiState: StateFlow<AssistantUiState> = _uiState.asStateFlow()

    init {
        // Welcome message
        _uiState.update {
            it.copy(
                messages = listOf(
                    ChatMessage(
                        content = "Hey! I'm your LifeOS assistant. Ask me about your spending, tasks, or schedule. 💡",
                        sender = MessageSender.ASSISTANT
                    )
                )
            )
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
                isProcessing = true
            )
        }

        viewModelScope.launch {
            try {
                // Small delay for natural feel
                delay(300)
                val response = repository.processMessage(text)
                _uiState.update {
                    it.copy(
                        messages = it.messages + response,
                        isProcessing = false
                    )
                }
            } catch (e: Exception) {
                val errorMessage = ChatMessage(
                    content = "Sorry, I ran into an error. Please try again.",
                    sender = MessageSender.ASSISTANT
                )
                _uiState.update {
                    it.copy(
                        messages = it.messages + errorMessage,
                        isProcessing = false
                    )
                }
            }
        }
    }

    fun sendSuggestedPrompt(prompt: String) {
        _uiState.update { it.copy(inputText = prompt) }
        sendMessage()
    }
}
