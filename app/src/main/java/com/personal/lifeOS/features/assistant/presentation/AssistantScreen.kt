package com.personal.lifeOS.features.assistant.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.personal.lifeOS.core.ui.designsystem.AssistantActionCard
import com.personal.lifeOS.core.ui.designsystem.InlineBanner
import com.personal.lifeOS.core.ui.designsystem.InlineBannerTone
import com.personal.lifeOS.ui.theme.AppSpacing

@Composable
fun AssistantScreen(viewModel: AssistantViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    var showClearChatConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(state.proposalResultMessage) {
        if (state.proposalResultMessage != null) {
            viewModel.clearProposalResultMessage()
        }
    }

    // Auto-scroll to latest message whenever new content arrives
    LaunchedEffect(state.messages.size, state.isProcessing) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.size - 1)
        }
    }

    // No Scaffold — InputBar manages its own bottom clearance via imePadding() and
    // a 72 dp transparent gap that lets the floating bottom nav bar show through.
    // Using Scaffold's bottomBar slot would double-count the clearance as scaffoldPadding,
    // which collapses the message list area and breaks keyboard handling.
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .statusBarsPadding()
                // imePadding here (not inside InputBar's Row chain) so the Column
                // shrinks cleanly when the keyboard opens, and the LazyColumn
                // keeps its weight-based height correctly.
                .imePadding(),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = AppSpacing.ScreenHorizontal,
                        vertical = AppSpacing.Section,
                    ),
        ) {
            AssistantHeader(
                isProcessing = state.isProcessing,
                onClearChat = { showClearChatConfirm = true },
            )
        }

        LazyColumn(
            state = listState,
            modifier =
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = AppSpacing.ScreenHorizontal),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.Section),
            // Small bottom padding so the last message doesn't sit flush against InputBar
            contentPadding = PaddingValues(bottom = 8.dp),
        ) {
            state.proposalResultMessage?.let { banner ->
                item {
                    InlineBanner(
                        message = banner,
                        tone = InlineBannerTone.INFO,
                    )
                }
            }

            items(state.messages) { message ->
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

        // InputBar placed directly in the Column — it owns its own imePadding() and
        // the 72 dp bottom clearance, so no Scaffold wrapper is needed.
        InputBar(
            text = state.inputText,
            onTextChange = { viewModel.updateInput(it) },
            onSend = { viewModel.sendMessage() },
            isProcessing = state.isProcessing,
        )
    }

    if (showClearChatConfirm) {
        AlertDialog(
            onDismissRequest = { showClearChatConfirm = false },
            title = { Text("Clear chat history?") },
            text = {
                Text("This will remove your current assistant conversation and start a fresh one.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearChatConfirm = false
                        viewModel.clearChat()
                    },
                ) {
                    Text("Clear")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearChatConfirm = false }) {
                    Text("Cancel")
                }
            },
        )
    }
}
