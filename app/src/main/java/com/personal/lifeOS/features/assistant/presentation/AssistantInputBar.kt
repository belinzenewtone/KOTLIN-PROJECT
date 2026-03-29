package com.personal.lifeOS.features.assistant.presentation

import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
    // When the keyboard is visible, the parent Column's imePadding() has already pushed
    // this bar up above the keyboard — no extra clearance is needed.
    // When the keyboard is hidden, we add clearance for the floating nav bar overlay
    // (~64dp bar + 8dp gap = 72dp). Animate the transition so the bar glides smoothly.
    val isImeVisible = WindowInsets.isImeVisible
    val floatingBarClearance by animateDpAsState(
        targetValue = if (isImeVisible) 0.dp else 72.dp,
        animationSpec = tween(durationMillis = 220, easing = EaseInOut),
        label = "floatingBarClearance",
    )

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                // navigationBarsPadding handles both gesture-swipe bar AND button-nav bar.
                // This is the ONLY inset that should be applied here — the keyboard inset
                // is handled by the parent Column's imePadding().
                .navigationBarsPadding()
                // Slide in extra clearance for the floating bottom nav only when
                // the keyboard is not visible.
                .padding(bottom = floatingBarClearance)
                .padding(horizontal = AppSpacing.ScreenHorizontal, vertical = 10.dp),
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
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
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
