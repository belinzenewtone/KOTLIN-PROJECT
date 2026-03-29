package com.personal.lifeOS.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ── Light mode — Stitch "Digital Sanctuary" palette (2025 refresh) ──────────
// Surfaces now carry a warm teal tint so every layer of the hierarchy feels
// part of the same colour family rather than disconnected blue-grey.
// Background (#F3F7F7) → surface (#FFFFFF) → surfaceVariant (#EAF1F1)
// Primary text on white: #191C1E (~15:1 contrast). Secondary: #3C4949 (~7:1).
private val LightColorScheme =
    lightColorScheme(
        primary = Primary,                      // #007070 — slightly fresher teal
        onPrimary = Color.White,
        primaryContainer = Color(0xFFB8E5E5),   // richer teal tint for chips/tags (was #CCEDED)
        onPrimaryContainer = Color(0xFF002020),
        secondary = Color(0xFF4A6060),          // slightly deeper for better contrast (was #5A6B6B)
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFD5EBEB), // cohesive with primaryContainer (was #E0EFEF)
        onSecondaryContainer = Color(0xFF0E2020),
        background = Color(0xFFF3F7F7),         // warm teal-tinted page (was #F7F9FC — cold blue)
        onBackground = Color(0xFF191C1E),       // near-black — excellent contrast
        surface = Color(0xFFFFFFFF),            // cards stay white for clear elevation
        onSurface = Color(0xFF191C1E),
        surfaceVariant = Color(0xFFEAF1F1),     // teal-tinted inner wells (was #F0F3F6)
        onSurfaceVariant = Color(0xFF3C4949),   // dark teal-grey ~7:1
        outline = Color(0xFFB0C8C8),            // slightly more teal (was #BBC9C8)
        outlineVariant = Color(0xFFD4E4E4),     // cohesive (was #DDE5E5)
        error = Error,
        onError = Color.White,
        errorContainer = Color(0xFFFFDAD6),
        onErrorContainer = Color(0xFF410002),
        inverseSurface = Color(0xFF2C3232),     // slight teal tint (was #2E3133)
        inverseOnSurface = Color(0xFFEDF2F2),   // slight teal tint (was #EFF1F4)
        inversePrimary = Color(0xFF4DD0D0),
        scrim = Color(0xFF000000),
    )

// ── Dark mode (2025 refresh) ──────────────────────────────────────────────────
// Surfaces carry a faint teal tint so dark-mode cards feel intentional rather
// than pure grey. Background: #0F1414, cards: #192222, elevated: #21292A.
// Primary text: #EDF2F2 (~13:1). Secondary: #B0C8C8 (~6:1).
private val DarkColorScheme =
    darkColorScheme(
        primary = Color(0xFF4DD0D0),            // lighter teal — visible on dark
        onPrimary = Color(0xFF002020),
        primaryContainer = Color(0xFF004E4E),   // slightly richer (was #004F4F)
        onPrimaryContainer = Color(0xFFBDE7E7), // matches light primaryContainer (was #CCEDED)
        secondary = Color(0xFFB0C8C8),          // slightly more teal (was #B0CECE)
        onSecondary = Color(0xFF1A3232),
        secondaryContainer = Color(0xFF2A4242), // teal-tinted (was #2D4545)
        onSecondaryContainer = Color(0xFFCCE7E7),
        background = Color(0xFF0F1414),         // deeper teal-dark (was #111416 — blue-grey)
        onBackground = Color(0xFFEDF2F2),       // slight teal tint (was #EFF1F4)
        surface = Color(0xFF192222),            // teal-tinted card (was #1C2022 — blue-grey)
        onSurface = Color(0xFFEDF2F2),
        surfaceVariant = Color(0xFF21292A),     // teal-tinted elevated (was #252A2D)
        onSurfaceVariant = Color(0xFFB0C8C8),   // muted teal — secondary labels (was #BBC9C8)
        outline = Color(0xFF4E6E6E),            // teal-tinted (was #52706F)
        outlineVariant = Color(0xFF2A3C3C),     // teal-tinted (was #2D3D3C)
        error = Color(0xFFFFB4AB),
        onError = Color(0xFF690005),
        errorContainer = Color(0xFF93000A),
        onErrorContainer = Color(0xFFFFDAD6),
        inverseSurface = Color(0xFFEDF2F2),
        inverseOnSurface = Color(0xFF2C3232),   // teal tint (was #2E3133)
        inversePrimary = Primary,
        scrim = Color(0xFF000000),
    )

private val HeadlineFamily = FontFamily.SansSerif
private val BodyFamily = FontFamily.SansSerif

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
                platformStyle = PlatformTextStyle(includeFontPadding = false),
            ),
        headlineLarge =
            TextStyle(
                fontFamily = HeadlineFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 28.sp,
                lineHeight = 34.sp,
                platformStyle = PlatformTextStyle(includeFontPadding = false),
            ),
        headlineMedium =
            TextStyle(
                fontFamily = HeadlineFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 22.sp,
                lineHeight = 30.sp,
                platformStyle = PlatformTextStyle(includeFontPadding = false),
            ),
        headlineSmall =
            TextStyle(
                fontFamily = HeadlineFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 20.sp,
                lineHeight = 28.sp,
                platformStyle = PlatformTextStyle(includeFontPadding = false),
            ),
        titleLarge =
            TextStyle(
                fontFamily = HeadlineFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 18.sp,
                lineHeight = 24.sp,
                platformStyle = PlatformTextStyle(includeFontPadding = false),
            ),
        titleMedium =
            TextStyle(
                fontFamily = HeadlineFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                lineHeight = 22.sp,
                platformStyle = PlatformTextStyle(includeFontPadding = false),
            ),
        titleSmall =
            TextStyle(
                fontFamily = HeadlineFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                platformStyle = PlatformTextStyle(includeFontPadding = false),
            ),
        bodyLarge =
            TextStyle(
                fontFamily = BodyFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 16.sp,
                lineHeight = 24.sp,
                platformStyle = PlatformTextStyle(includeFontPadding = false),
            ),
        bodyMedium =
            TextStyle(
                fontFamily = BodyFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                platformStyle = PlatformTextStyle(includeFontPadding = false),
            ),
        bodySmall =
            TextStyle(
                fontFamily = BodyFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                platformStyle = PlatformTextStyle(includeFontPadding = false),
            ),
        labelLarge =
            TextStyle(
                fontFamily = BodyFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                platformStyle = PlatformTextStyle(includeFontPadding = false),
            ),
        labelMedium =
            TextStyle(
                fontFamily = BodyFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                platformStyle = PlatformTextStyle(includeFontPadding = false),
            ),
        labelSmall =
            TextStyle(
                fontFamily = BodyFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                platformStyle = PlatformTextStyle(includeFontPadding = false),
            ),
    )

private val LifeOSShapes =
    Shapes(
        extraSmall = RoundedCornerShape(10.dp),
        small = RoundedCornerShape(14.dp),
        medium = RoundedCornerShape(18.dp),
        large = RoundedCornerShape(24.dp),
        extraLarge = RoundedCornerShape(32.dp),
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
        shapes = LifeOSShapes,
        content = content,
    )
}
