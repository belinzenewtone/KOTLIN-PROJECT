package com.personal.lifeOS.features.assistant.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.personal.lifeOS.features.assistant.domain.model.ChatMessage
import com.personal.lifeOS.features.assistant.domain.model.MessageSender
import com.personal.lifeOS.features.assistant.domain.model.suggestedPrompts
import com.personal.lifeOS.ui.components.GlassCard
import com.personal.lifeOS.ui.theme.*

@Composable
fun AssistantScreen(
    viewModel: AssistantViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()

    // Auto-scroll to bottom on new message
    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Primary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.SmartToy,
                    contentDescription = null,
                    tint = Primary,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text("LifeOS Assistant", style = MaterialTheme.typography.titleLarge)
                Text(
                    if (state.isProcessing) "Thinking..." else "Online",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (state.isProcessing) Warning else Accent
                )
            }
        }

        // Messages
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(state.messages, key = { it.id }) { message ->
                ChatBubble(message = message)
            }

            // Typing indicator
            if (state.isProcessing) {
                item {
                    TypingIndicator()
                }
            }

            // Suggested prompts (show when few messages)
            if (state.messages.size <= 1) {
                item {
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Try asking:",
                        style = MaterialTheme.typography.labelLarge,
                        color = TextTertiary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    SuggestedPrompts(
                        onSelect = { viewModel.sendSuggestedPrompt(it) }
                    )
                }
            }
        }

        // Input bar
        InputBar(
            text = state.inputText,
            onTextChange = { viewModel.updateInput(it) },
            onSend = { viewModel.sendMessage() },
            isProcessing = state.isProcessing
        )
    }
}

@Composable
private fun ChatBubble(message: ChatMessage) {
    val isUser = message.sender == MessageSender.USER
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Primary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.SmartToy,
                    contentDescription = null,
                    tint = Primary,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(Modifier.width(8.dp))
        }

        Box(
            modifier = Modifier
                .widthIn(max = screenWidth * 0.78f)
                .clip(
                    RoundedCornerShape(
                        topStart = 20.dp,
                        topEnd = 20.dp,
                        bottomStart = if (isUser) 20.dp else 4.dp,
                        bottomEnd = if (isUser) 4.dp else 20.dp
                    )
                )
                .background(
                    if (isUser) Primary.copy(alpha = 0.85f)
                    else GlassWhite
                )
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text(
                text = message.content,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isUser) BackgroundDark else TextPrimary,
                fontWeight = if (isUser) FontWeight.Medium else FontWeight.Normal
            )
        }
    }
}

@Composable
private fun TypingIndicator() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(Primary.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Filled.SmartToy,
                contentDescription = null,
                tint = Primary,
                modifier = Modifier.size(16.dp)
            )
        }
        Spacer(Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp, 20.dp, 20.dp, 4.dp))
                .background(GlassWhite)
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                repeat(3) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(TextTertiary)
                    )
                }
            }
        }
    }
}

@Composable
private fun SuggestedPrompts(onSelect: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        suggestedPrompts.chunked(2).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { prompt ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(GlassWhite)
                            .clickable { onSelect(prompt) }
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Text(
                            prompt,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }
                // Fill remaining space if odd number
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun InputBar(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    isProcessing: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceDark)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .padding(bottom = 72.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = onTextChange,
            modifier = Modifier.weight(1f),
            placeholder = { Text("Message LifeOS...", color = TextTertiary) },
            shape = RoundedCornerShape(24.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Primary,
                unfocusedBorderColor = GlassBorder,
                focusedContainerColor = GlassWhite,
                unfocusedContainerColor = GlassWhite,
                cursorColor = Primary
            ),
            singleLine = false,
            maxLines = 4
        )

        IconButton(
            onClick = onSend,
            enabled = text.isNotBlank() && !isProcessing,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(
                    if (text.isNotBlank() && !isProcessing) Primary
                    else Primary.copy(alpha = 0.3f)
                )
        ) {
            if (isProcessing) {
                CircularProgressIndicator(
                    color = TextPrimary,
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    Icons.Filled.Send,
                    contentDescription = "Send",
                    tint = TextPrimary
                )
            }
        }
    }
}
