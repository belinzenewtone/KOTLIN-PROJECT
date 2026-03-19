package com.personal.lifeOS.core.ui.designsystem

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class AppColorRoles(
    val primary: Color,
    val primaryContainer: Color,
    val surface: Color,
    val surfaceContainerLow: Color,
    val surfaceContainerLowest: Color,
    val onSurface: Color,
    val onSurfaceVariant: Color,
    val outlineVariant: Color,
    val success: Color,
    val warning: Color,
    val error: Color,
)

data class AppTypographyScale(
    val display: TextUnit,
    val headline: TextUnit,
    val title: TextUnit,
    val body: TextUnit,
    val label: TextUnit,
    val mono: TextUnit,
)

data class AppSpacingScale(
    val xs: Dp,
    val sm: Dp,
    val md: Dp,
    val lg: Dp,
    val xl: Dp,
)

data class AppRadiusScale(
    val md: Dp,
    val lg: Dp,
    val xl: Dp,
)

data class AppElevationSpec(
    val card: Dp,
    val floating: Dp,
    val sheet: Dp,
)

data class AppMotionSpec(
    val fastMs: Int,
    val standardMs: Int,
    val slowMs: Int,
)

object AppDesignTokens {
    // Stitch-first color roles.
    val colors =
        AppColorRoles(
            primary = Color(0xFF006A6A),
            primaryContainer = Color(0xFF00A8A8),
            surface = Color(0xFFF7F9FC),
            surfaceContainerLow = Color(0xFFF2F4F7),
            surfaceContainerLowest = Color(0xFFFFFFFF),
            onSurface = Color(0xFF191C1E),
            onSurfaceVariant = Color(0xFF3C4949),
            outlineVariant = Color(0xFFBBC9C8),
            success = Color(0xFF2E7D32),
            warning = Color(0xFFEF6C00),
            error = Color(0xFFBA1A1A),
        )

    val typography =
        AppTypographyScale(
            display = 48.sp,
            headline = 28.sp,
            title = 18.sp,
            body = 15.sp,
            label = 12.sp,
            mono = 14.sp,
        )

    val spacing =
        AppSpacingScale(
            xs = 6.dp,
            sm = 10.dp,
            md = 16.dp,
            lg = 24.dp,
            xl = 32.dp,
        )

    val radius =
        AppRadiusScale(
            md = 16.dp,
            lg = 24.dp,
            xl = 32.dp,
        )

    val elevation =
        AppElevationSpec(
            card = 2.dp,
            floating = 10.dp,
            sheet = 16.dp,
        )

    val motion =
        AppMotionSpec(
            fastMs = 120,
            standardMs = 220,
            slowMs = 320,
        )
}
