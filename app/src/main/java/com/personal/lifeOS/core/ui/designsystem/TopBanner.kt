package com.personal.lifeOS.core.ui.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * TopBanner - High-priority notification banner placed at the top of screens.
 *
 * Used for:
 * - Sync status (syncing, failed, offline)
 * - Import alerts (processing, completed, errors)
 * - Permission prompts (location, SMS, biometric)
 * - System alerts (low storage, battery, app updates)
 *
 * Design:
 * - Full width, placed above page title
 * - Colored background based on tone (error, warning, info, success)
 * - Left icon indicates type
 * - Right close button (optional)
 * - Centered text, properly sized and spaced
 * - Non-intrusive but visible
 */
@Composable
fun TopBanner(
    message: String,
    modifier: Modifier = Modifier,
    tone: TopBannerTone = TopBannerTone.INFO,
    title: String? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    onDismiss: (() -> Unit)? = null,
) {
    val (backgroundColor, textColor, iconColor, icon) = when (tone) {
        TopBannerTone.ERROR -> {
            TopBannerColor(
                bg = Color(0xFFFFEBEE),
                text = Color(0xFFB71C1C),
                icon = Color(0xFFC62828),
                imageVector = Icons.Filled.ErrorOutline,
            )
        }

        TopBannerTone.WARNING -> {
            TopBannerColor(
                bg = Color(0xFFFFF3E0),
                text = Color(0xFFE65100),
                icon = Color(0xFFEF6C00),
                imageVector = Icons.Filled.Warning,
            )
        }

        TopBannerTone.SUCCESS -> {
            TopBannerColor(
                bg = Color(0xFFE8F5E9),
                text = Color(0xFF1B5E20),
                icon = Color(0xFF2E7D32),
                imageVector = Icons.Filled.Info,
            )
        }

        TopBannerTone.INFO -> {
            TopBannerColor(
                bg = Color(0xFFE3F2FD),
                text = Color(0xFF0D47A1),
                icon = Color(0xFF1565C0),
                imageVector = Icons.Filled.Info,
            )
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .padding(vertical = 12.dp, horizontal = 16.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            // Icon
            Icon(
                imageVector = icon,
                contentDescription = tone.name,
                tint = iconColor,
                modifier = Modifier
                    .padding(top = 2.dp)
                    .then(if (title != null) Modifier else Modifier),
            )

            // Content
            Column(
                modifier = Modifier
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                // Title (optional)
                if (title != null) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelLarge,
                        color = textColor,
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                // Message
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = textColor,
                )

                // Action (optional)
                if (actionLabel != null && onAction != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onAction)
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = actionLabel,
                            style = MaterialTheme.typography.labelMedium,
                            color = textColor,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }

            // Close button (optional)
            if (onDismiss != null) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.padding(0.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Dismiss",
                        tint = textColor,
                    )
                }
            }
        }
    }
}

/**
 * Internal color configuration for TopBanner.
 */
private data class TopBannerColor(
    val bg: Color,
    val text: Color,
    val icon: Color,
    val imageVector: ImageVector,
)

/**
 * TopBanner tone/severity levels.
 */
enum class TopBannerTone {
    /**
     * Error/critical state - requires user attention.
     * Red background.
     *
     * Use for:
     * - Sync failures
     * - Import errors
     * - Critical system issues
     */
    ERROR,

    /**
     * Warning state - important but not critical.
     * Orange background.
     *
     * Use for:
     * - Low storage
     * - Sync slow
     * - Partial failures
     */
    WARNING,

    /**
     * Success/positive state.
     * Green background.
     *
     * Use for:
     * - Sync complete
     * - Import successful
     * - Permissions granted
     */
    SUCCESS,

    /**
     * Information/neutral state.
     * Blue background.
     *
     * Use for:
     * - General info
     * - Syncing in progress
     * - Tips and hints
     */
    INFO,
}
