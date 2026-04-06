package com.personal.lifeOS.core.ui.designsystem

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.personal.lifeOS.ui.theme.Primary

/**
 * Telegram-style toggle switch.
 *
 * Visual spec:
 *  • Checked   — track: app brand blue (Primary, identical in light & dark),
 *                thumb: white, no border — matches Telegram's consistent switch
 *  • Unchecked — track: onSurfaceVariant at 20 % opacity (visible in both
 *                themes without looking washed-out), thumb: white, no border
 *
 * Uses the static [Primary] constant rather than [MaterialTheme.colorScheme.primary]
 * so the track stays the same vivid blue in dark mode (where the theme primary
 * shifts to a lighter tint for text contrast purposes).
 *
 * Drop-in replacement for Material 3 [Switch] — identical API.
 */
@Composable
fun LifeOSSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val trackOff = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.20f)
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        enabled = enabled,
        colors = SwitchDefaults.colors(
            // ── Checked state ────────────────────────────────────────────────
            checkedTrackColor = Primary,
            checkedThumbColor = Color.White,
            checkedBorderColor = Color.Transparent,
            checkedIconColor = Color.Transparent,
            // ── Unchecked state ──────────────────────────────────────────────
            uncheckedTrackColor = trackOff,
            uncheckedThumbColor = Color.White,
            uncheckedBorderColor = Color.Transparent,
            uncheckedIconColor = Color.Transparent,
            // ── Disabled states ──────────────────────────────────────────────
            disabledCheckedTrackColor = Primary.copy(alpha = 0.38f),
            disabledCheckedThumbColor = Color.White.copy(alpha = 0.60f),
            disabledCheckedBorderColor = Color.Transparent,
            disabledUncheckedTrackColor = trackOff.copy(alpha = 0.38f),
            disabledUncheckedThumbColor = Color.White.copy(alpha = 0.60f),
            disabledUncheckedBorderColor = Color.Transparent,
        ),
    )
}
