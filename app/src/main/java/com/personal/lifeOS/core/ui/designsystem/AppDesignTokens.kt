package com.personal.lifeOS.core.ui.designsystem

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.personal.lifeOS.ui.theme.Error
import com.personal.lifeOS.ui.theme.Primary
import com.personal.lifeOS.ui.theme.Success
import com.personal.lifeOS.ui.theme.Warning

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

    /**
     * Theme-aware color roles derived from the active MaterialTheme.
     * Always use this inside a @Composable context so colors adapt to
     * light / dark mode automatically.
     */
    val colors: AppColorRoles
        @Composable
        @ReadOnlyComposable
        get() = AppColorRoles(
            primary = MaterialTheme.colorScheme.primary,
            primaryContainer = MaterialTheme.colorScheme.primaryContainer,
            surface = MaterialTheme.colorScheme.surface,
            surfaceContainerLow = MaterialTheme.colorScheme.surfaceVariant,
            surfaceContainerLowest = MaterialTheme.colorScheme.surface,
            onSurface = MaterialTheme.colorScheme.onSurface,
            onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant,
            outlineVariant = MaterialTheme.colorScheme.outlineVariant,
            success = Success,
            warning = Warning,
            error = Error,
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
