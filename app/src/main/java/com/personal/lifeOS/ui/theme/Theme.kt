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

private val DarkColorScheme =
    darkColorScheme(
        primary = Accent,
        onPrimary = Color(0xFF002020),
        secondary = Color(0xFFDEE2EF),
        onSecondary = Color.White,
        background = Color(0xFF111416),
        onBackground = Color(0xFFEFF1F4),
        surface = Color(0xFF1B1F22),
        onSurface = Color(0xFFEFF1F4),
        surfaceVariant = Color(0xFF2D3133),
        onSurfaceVariant = Color(0xFFBBC9C8),
        error = Error,
        onError = Color.White,
    )

private val LightColorScheme =
    lightColorScheme(
        primary = Primary,
        onPrimary = Color.White,
        secondary = Accent,
        onSecondary = Color(0xFF003636),
        background = BackgroundDark,
        onBackground = TextPrimary,
        surface = SurfaceDark,
        onSurface = TextPrimary,
        surfaceVariant = Color(0xFFE0E3E6),
        onSurfaceVariant = Color(0xFF3C4949),
        error = Error,
        onError = Color.White,
    )

private val HeadlineFamily = FontFamily.SansSerif
private val BodyFamily = FontFamily.SansSerif
private val MonoFamily = FontFamily.Monospace

private val LifeOSTypography =
    Typography(
        displayLarge =
            TextStyle(
                fontFamily = HeadlineFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 36.sp,
                lineHeight = 42.sp,
                color = TextPrimary,
            ),
        headlineLarge =
            TextStyle(
                fontFamily = HeadlineFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 28.sp,
                lineHeight = 34.sp,
                color = TextPrimary,
            ),
        headlineMedium =
            TextStyle(
                fontFamily = HeadlineFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 22.sp,
                lineHeight = 30.sp,
                color = TextPrimary,
            ),
        titleLarge =
            TextStyle(
                fontFamily = HeadlineFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 18.sp,
                lineHeight = 24.sp,
                color = TextPrimary,
            ),
        titleMedium =
            TextStyle(
                fontFamily = HeadlineFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp,
                lineHeight = 22.sp,
                color = TextPrimary,
            ),
        bodyLarge =
            TextStyle(
                fontFamily = BodyFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 16.sp,
                lineHeight = 24.sp,
                color = TextSecondary,
            ),
        bodyMedium =
            TextStyle(
                fontFamily = BodyFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = TextSecondary,
            ),
        labelLarge =
            TextStyle(
                fontFamily = MonoFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = TextPrimary,
            ),
        labelSmall =
            TextStyle(
                fontFamily = MonoFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                color = TextTertiary,
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
