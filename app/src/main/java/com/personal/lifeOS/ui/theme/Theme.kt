package com.personal.lifeOS.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// ── Light mode — Stitch "Digital Sanctuary" palette ─────────────────────────
// Background hierarchy: background (#F7F9FC) → surface (#FFFFFF) → surfaceVariant (#F0F3F6)
// Primary text on white: #191C1E (~15:1 contrast). Secondary: #3C4949 (~7:1).
private val LightColorScheme =
    lightColorScheme(
        primary = Primary,                      // #006A6A  teal
        onPrimary = Color.White,
        primaryContainer = Color(0xFFCCEDED),   // light teal tint for chips/tags
        onPrimaryContainer = Color(0xFF002020),
        secondary = Color(0xFF5A6B6B),          // muted teal-grey
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFE0EFEF),
        onSecondaryContainer = Color(0xFF0E2020),
        background = Color(0xFFF7F9FC),         // lightest page background
        onBackground = Color(0xFF191C1E),       // near-black — excellent contrast
        surface = Color(0xFFFFFFFF),            // cards and elevated surfaces = white
        onSurface = Color(0xFF191C1E),
        surfaceVariant = Color(0xFFF0F3F6),     // subtle variant for inner wells
        onSurfaceVariant = Color(0xFF3C4949),   // dark teal-grey ~7:1
        outline = Color(0xFFBBC9C8),
        outlineVariant = Color(0xFFDDE5E5),
        error = Error,
        onError = Color.White,
        errorContainer = Color(0xFFFFDAD6),
        onErrorContainer = Color(0xFF410002),
        inverseSurface = Color(0xFF2E3133),
        inverseOnSurface = Color(0xFFEFF1F4),
        inversePrimary = Color(0xFF4DD0D0),
        scrim = Color(0xFF000000),
    )

// ── Dark mode ────────────────────────────────────────────────────────────────
// Background: #111416, surface cards: #1C2022, elevated: #252A2D
// Primary text on dark: #EFF1F4 (~13:1). Secondary: #BBC9C8 (~6:1).
private val DarkColorScheme =
    darkColorScheme(
        primary = Color(0xFF4DD0D0),            // lighter teal — visible on dark
        onPrimary = Color(0xFF002020),
        primaryContainer = Color(0xFF004F4F),
        onPrimaryContainer = Color(0xFFCCEDED),
        secondary = Color(0xFFB0CECE),
        onSecondary = Color(0xFF1A3333),
        secondaryContainer = Color(0xFF2D4545),
        onSecondaryContainer = Color(0xFFCCE7E7),
        background = Color(0xFF111416),         // deep dark background
        onBackground = Color(0xFFEFF1F4),       // near-white
        surface = Color(0xFF1C2022),            // card surfaces — slightly lighter than bg
        onSurface = Color(0xFFEFF1F4),
        surfaceVariant = Color(0xFF252A2D),     // elevated inner sections
        onSurfaceVariant = Color(0xFFBBC9C8),   // muted light teal — secondary labels
        outline = Color(0xFF52706F),
        outlineVariant = Color(0xFF2D3D3C),
        error = Color(0xFFFFB4AB),
        onError = Color(0xFF690005),
        errorContainer = Color(0xFF93000A),
        onErrorContainer = Color(0xFFFFDAD6),
        inverseSurface = Color(0xFFEFF1F4),
        inverseOnSurface = Color(0xFF2E3133),
        inversePrimary = Primary,
        scrim = Color(0xFF000000),
    )

private val HeadlineFamily = FontFamily.SansSerif
private val BodyFamily = FontFamily.SansSerif
private val MonoFamily = FontFamily.Monospace

/**
 * Typography must NOT have hardcoded colors — Material3 automatically applies
 * the correct onSurface/onBackground color from the active colorScheme.
 * Hardcoded colors would lock text to light-mode values and break dark mode.
 */
private val LifeOSTypography =
    Typography(
        displayLarge =
            TextStyle(
                fontFamily = HeadlineFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 36.sp,
                lineHeight = 42.sp,
            ),
        headlineLarge =
            TextStyle(
                fontFamily = HeadlineFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 28.sp,
                lineHeight = 34.sp,
            ),
        headlineMedium =
            TextStyle(
                fontFamily = HeadlineFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 22.sp,
                lineHeight = 30.sp,
            ),
        headlineSmall =
            TextStyle(
                fontFamily = HeadlineFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 20.sp,
                lineHeight = 28.sp,
            ),
        titleLarge =
            TextStyle(
                fontFamily = HeadlineFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 18.sp,
                lineHeight = 24.sp,
            ),
        titleMedium =
            TextStyle(
                fontFamily = HeadlineFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp,
                lineHeight = 22.sp,
            ),
        titleSmall =
            TextStyle(
                fontFamily = HeadlineFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                lineHeight = 20.sp,
            ),
        bodyLarge =
            TextStyle(
                fontFamily = BodyFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 16.sp,
                lineHeight = 24.sp,
            ),
        bodyMedium =
            TextStyle(
                fontFamily = BodyFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 14.sp,
                lineHeight = 20.sp,
            ),
        bodySmall =
            TextStyle(
                fontFamily = BodyFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 12.sp,
                lineHeight = 16.sp,
            ),
        labelLarge =
            TextStyle(
                fontFamily = MonoFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                lineHeight = 20.sp,
            ),
        labelMedium =
            TextStyle(
                fontFamily = MonoFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 13.sp,
                lineHeight = 18.sp,
            ),
        labelSmall =
            TextStyle(
                fontFamily = MonoFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 12.sp,
                lineHeight = 16.sp,
            ),
    )

@Composable
fun LifeOSTheme(
    themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    content: @Composable () -> Unit,
) {
    val useDarkTheme =
        when (themeMode) {
            AppThemeMode.SYSTEM -> isSystemInDarkTheme()
            AppThemeMode.DARK -> true
            AppThemeMode.LIGHT -> false
        }

    MaterialTheme(
        colorScheme = if (useDarkTheme) DarkColorScheme else LightColorScheme,
        typography = LifeOSTypography,
        content = content,
    )
}
