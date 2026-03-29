package com.personal.lifeOS.core.permissions

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.personal.lifeOS.core.ui.designsystem.AppDesignTokens

/**
 * A polished bottom-anchored permission rationale card that slides up from the
 * bottom of the screen. Shown once per permission group — if the user dismisses
 * via "Not now", we never ask again (tracked via [AppSettingsStore]).
 *
 * @param visible     Drive with a remembered state flag from the orchestrator.
 * @param icon        Relevant icon for the permission being requested.
 * @param title       Short, benefit-focused title (e.g. "Stay on top of reminders").
 * @param description One sentence explaining why this permission helps the user.
 * @param allowLabel  Label for the primary action button (default "Allow").
 * @param denyLabel   Label for the secondary dismiss button (default "Not now").
 * @param onAllow     Called when the user taps Allow — launch the system request here.
 * @param onDeny      Called when the user taps Not now — mark as asked & never show again.
 */
@Composable
fun PermissionRationaleCard(
    visible: Boolean,
    icon: ImageVector,
    title: String,
    description: String,
    allowLabel: String = "Allow",
    denyLabel: String = "Not now",
    onAllow: () -> Unit,
    onDeny: () -> Unit,
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(260, easing = EaseInOut)) +
            slideInVertically(tween(300, easing = EaseInOut)) { it / 2 },
        exit = fadeOut(tween(200, easing = EaseInOut)) +
            slideOutVertically(tween(240, easing = EaseInOut)) { it / 2 },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(AppDesignTokens.radius.lg))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant,
                        shape = RoundedCornerShape(AppDesignTokens.radius.lg),
                    )
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Icon + title row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(26.dp),
                    )
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }

                // Description
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedButton(onClick = onDeny) {
                        Text(denyLabel, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(onClick = onAllow) {
                        Text(allowLabel)
                    }
                }
            }
        }
    }
}
