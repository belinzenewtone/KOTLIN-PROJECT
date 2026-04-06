package com.personal.lifeOS.core.ui.designsystem

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/**
 * Telegram-style toggle switch.
 *
 * Visual spec:
 *  • Checked   — track: primary blue, thumb: white, no border
 *  • Unchecked — track: surfaceVariant (muted grey), thumb: white, no border
 *
 * Drop-in replacement for Material 3 [Switch] — identical API, consistent
 * styling across the whole app without manual `colors =` repetition.
 */
@Composable
fun LifeOSSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        enabled = enabled,
        colors = SwitchDefaults.colors(
            // ── Checked state ────────────────────────────────────────────────
            checkedTrackColor = MaterialTheme.colorScheme.primary,
            checkedThumbColor = Color.White,
            checkedBorderColor = Color.Transparent,
            checkedIconColor = Color.Transparent,
            // ── Unchecked state ──────────────────────────────────────────────
            uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
            uncheckedThumbColor = Color.White,
            uncheckedBorderColor = Color.Transparent,
            uncheckedIconColor = Color.Transparent,
            // ── Disabled states ──────────────────────────────────────────────
            disabledCheckedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.38f),
            disabledCheckedThumbColor = Color.White.copy(alpha = 0.60f),
            disabledCheckedBorderColor = Color.Transparent,
            disabledUncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f),
            disabledUncheckedThumbColor = Color.White.copy(alpha = 0.60f),
            disabledUncheckedBorderColor = Color.Transparent,
        ),
    )
}
