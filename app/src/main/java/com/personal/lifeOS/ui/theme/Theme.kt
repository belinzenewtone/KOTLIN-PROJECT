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
        primary = Primary,
        onPrimary = Color.White,
        primaryContainer = Color(0xFFD8ECFF),
        onPrimaryContainer = Color(0xFF002B4A),
        secondary = Color(0xFF4E5F70),
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFDBE5F1),
        onSecondaryContainer = Color(0xFF1B2A37),
        background = Color(0xFFF6F8FB),
        onBackground = Color(0xFF161A1D),
        surface = Color(0xFFFFFFFF),
        onSurface = Color(0xFF161A1D),
        surfaceVariant = Color(0xFFEDF2F7),
        onSurfaceVariant = Color(0xFF495662),
        outline = Color(0xFFB8C3CF),
        outlineVariant = Color(0xFFD7E0EA),
        error = Error,
        onError = Color.White,
        errorContainer = Color(0xFFFFDAD6),
        onErrorContainer = Color(0xFF410002),
        inverseSurface = Color(0xFF1A2028),
        inverseOnSurface = Color(0xFFEAF0F7),
        inversePrimary = Color(0xFF8FD1FF),
        scrim = Color(0xFF000000),
    )
// ── Dark mode (2025 refresh) ──────────────────────────────────────────────────
// Surfaces carry a faint teal tint so dark-mode cards feel intentional rather
// than pure grey. Background: #0F1414, cards: #192222, elevated: #21292A.
// Primary text: #EDF2F2 (~13:1). Secondary: #B0C8C8 (~6:1).
private val DarkColorScheme =
    darkColorScheme(
        primary = Color(0xFF57B5FF),
        onPrimary = Color(0xFF00233D),
        primaryContainer = Color(0xFF103A59),
        onPrimaryContainer = Color(0xFFD0E9FF),
        secondary = Color(0xFFAEC8DD),
        onSecondary = Color(0xFF1B3447),
        secondaryContainer = Color(0xFF273E50),
        onSecondaryContainer = Color(0xFFD4E7F5),
        background = Color(0xFF0B0F14),
        onBackground = Color(0xFFE8EDF3),
        surface = Color(0xFF11161C),
        onSurface = Color(0xFFE8EDF3),
        surfaceVariant = Color(0xFF1A2027),
        onSurfaceVariant = Color(0xFFA9B4BF),
        outline = Color(0xFF3C4854),
        outlineVariant = Color(0xFF28323B),
        error = Color(0xFFFFB4AB),
        onError = Color(0xFF690005),
        errorContainer = Color(0xFF93000A),
        onErrorContainer = Color(0xFFFFDAD6),
        inverseSurface = Color(0xFFE8EDF3),
        inverseOnSurface = Color(0xFF1A2028),
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
        extraSmall = RoundedCornerShape(8.dp),
        small = RoundedCornerShape(12.dp),
        medium = RoundedCornerShape(16.dp),
        large = RoundedCornerShape(20.dp),
        extraLarge = RoundedCornerShape(28.dp),
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
