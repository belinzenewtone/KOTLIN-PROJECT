package com.personal.lifeOS.features.assistant.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.personal.lifeOS.core.ui.designsystem.AppDesignTokens
import com.personal.lifeOS.ui.theme.AppSpacing

@Composable
@OptIn(ExperimentalLayoutApi::class)
internal fun InputBar(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    isProcessing: Boolean,
) {
    val isImeVisible = WindowInsets.isImeVisible
    val bottomClearance = if (isImeVisible) 8.dp else 48.dp

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(start = AppSpacing.ScreenHorizontal, end = AppSpacing.ScreenHorizontal, top = 10.dp)
                .navigationBarsPadding()
                .padding(bottom = bottomClearance)
                // imePadding removed — the parent Column in AssistantScreen owns it.
                // Keeping it here (before clip/background) caused the background surface
                // to expand into the keyboard region, making the box stretch infinitely.
                .clip(RoundedCornerShape(AppDesignTokens.radius.lg))
                .background(MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.92f))
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(AppDesignTokens.radius.lg),
                )
                .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = onTextChange,
            modifier = Modifier.weight(1f),
            placeholder = { Text("Message BELTECH...", color = MaterialTheme.colorScheme.onSurfaceVariant) },
            shape = RoundedCornerShape(AppDesignTokens.radius.lg),
            colors =
                OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    cursorColor = MaterialTheme.colorScheme.primary,
                ),
            singleLine = false,
            maxLines = 4,
        )

        IconButton(
            onClick = onSend,
            enabled = text.isNotBlank() && !isProcessing,
            modifier =
                Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        if (text.isNotBlank() && !isProcessing) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                        },
                    ),
        ) {
            if (isProcessing) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                )
            } else {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send",
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}
