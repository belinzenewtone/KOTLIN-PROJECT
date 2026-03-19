package com.personal.lifeOS.features.assistant.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.personal.lifeOS.ui.theme.AppSpacing
import com.personal.lifeOS.ui.theme.GlassBorder
import com.personal.lifeOS.ui.theme.GlassWhite
import com.personal.lifeOS.ui.theme.Primary
import com.personal.lifeOS.ui.theme.SurfaceDark
import com.personal.lifeOS.ui.theme.TextPrimary
import com.personal.lifeOS.ui.theme.TextTertiary

@Composable
internal fun InputBar(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    isProcessing: Boolean,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(SurfaceDark)
                .padding(horizontal = AppSpacing.ScreenHorizontal, vertical = AppSpacing.Section)
                .padding(bottom = AppSpacing.BottomSafe)
                .navigationBarsPadding()
                .imePadding(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = onTextChange,
            modifier = Modifier.weight(1f),
            placeholder = { Text("Message BELTECH...", color = TextTertiary) },
            shape = RoundedCornerShape(24.dp),
            colors =
                OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Primary,
                    unfocusedBorderColor = GlassBorder,
                    focusedContainerColor = GlassWhite,
                    unfocusedContainerColor = GlassWhite,
                    cursorColor = Primary,
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
                            Primary
                        } else {
                            Primary.copy(alpha = 0.3f)
                        },
                    ),
        ) {
            if (isProcessing) {
                CircularProgressIndicator(
                    color = TextPrimary,
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                )
            } else {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send",
                    tint = TextPrimary,
                )
            }
        }
    }
}
