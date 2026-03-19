package com.personal.lifeOS.features.assistant.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.personal.lifeOS.core.ui.designsystem.AssistantActionCard
import com.personal.lifeOS.core.ui.designsystem.InlineBanner
import com.personal.lifeOS.core.ui.designsystem.InlineBannerTone
import com.personal.lifeOS.ui.theme.AppSpacing
import com.personal.lifeOS.ui.theme.BackgroundDark

@Composable
fun AssistantScreen(viewModel: AssistantViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()

    LaunchedEffect(state.proposalResultMessage) {
        if (state.proposalResultMessage != null) {
            viewModel.clearProposalResultMessage()
        }
    }

    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.size - 1)
        }
    }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(BackgroundDark)
                .statusBarsPadding(),
    ) {
        AssistantHeader(isProcessing = state.isProcessing)

        LazyColumn(
            state = listState,
            modifier =
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = AppSpacing.ScreenHorizontal),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.Section),
        ) {
            state.proposalResultMessage?.let { banner ->
                item {
                    InlineBanner(
                        message = banner,
                        tone = InlineBannerTone.INFO,
                    )
                }
            }

            items(state.messages, key = { it.id }) { message ->
                ChatBubble(message = message)
            }

            state.pendingProposal?.let { proposal ->
                item {
                    AssistantActionCard(
                        proposal = proposal.toUiModel(),
                        onApprove = viewModel::approvePendingProposal,
                        onReject = viewModel::rejectPendingProposal,
                    )
                }
            }

            if (state.isProcessing) {
                item { TypingIndicator() }
            }

            if (state.messages.size <= 1) {
                item {
                    AssistantSuggestedPrompts(
                        onSelect = { viewModel.sendSuggestedPrompt(it) },
                    )
                }
            }
        }

        InputBar(
            text = state.inputText,
            onTextChange = { viewModel.updateInput(it) },
            onSend = { viewModel.sendMessage() },
            isProcessing = state.isProcessing,
        )
    }
}
