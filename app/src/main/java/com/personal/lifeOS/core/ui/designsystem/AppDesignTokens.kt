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

enum class AppSemanticTone {
    INFO,
    SUCCESS,
    WARNING,
    ERROR,
}

data class AppSemanticColors(
    val container: Color,
    val onContainer: Color,
    val icon: Color,
)

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
    val sm: Dp,
    val md: Dp,
    val lg: Dp,
    val xl: Dp,
    val pill: Dp,
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
            // Use the proper M3 surface-container tokens so cards have clear tonal hierarchy
            // in both light and dark mode — no manual tinting required.
            surfaceContainerLow = MaterialTheme.colorScheme.surfaceContainerLow,
            surfaceContainerLowest = MaterialTheme.colorScheme.surfaceContainerLowest,
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
            xs = 4.dp,
            sm = 8.dp,
            md = 14.dp,
            lg = 20.dp,
            xl = 28.dp,
        )

    val radius =
        AppRadiusScale(
            sm = 10.dp,
            md = 14.dp,
            lg = 18.dp,
            xl = 24.dp,
            pill = 999.dp,
        )

    // Shared floating bottom-nav geometry so overlays (assistant input, sheets)
    // can align to it consistently without trial-and-error values.
    val floatingNavBarHeight = 58.dp
    val floatingNavBarBottomOffset = 4.dp
    val assistantInputHairlineGap = 1.dp

    val elevation =
        AppElevationSpec(
            card = 1.dp,
            floating = 8.dp,
            sheet = 12.dp,
        )

    val motion =
        AppMotionSpec(
            fastMs = 100,
            standardMs = 180,
            slowMs = 260,
        )

    @Composable
    @ReadOnlyComposable
    fun semanticColors(tone: AppSemanticTone): AppSemanticColors {
        val scheme = MaterialTheme.colorScheme
        return when (tone) {
            AppSemanticTone.INFO ->
                AppSemanticColors(
                    container = scheme.primaryContainer.copy(alpha = 0.56f),
                    onContainer = scheme.onSurface,
                    icon = scheme.primary,
                )
            AppSemanticTone.SUCCESS ->
                AppSemanticColors(
                    container = scheme.secondaryContainer.copy(alpha = 0.50f),
                    onContainer = scheme.onSurface,
                    icon = colors.success,
                )
            AppSemanticTone.WARNING ->
                AppSemanticColors(
                    container = scheme.tertiaryContainer.copy(alpha = 0.58f),
                    onContainer = scheme.onSurface,
                    icon = colors.warning,
                )
            AppSemanticTone.ERROR ->
                AppSemanticColors(
                    container = scheme.errorContainer.copy(alpha = 0.62f),
                    onContainer = scheme.onSurface,
                    icon = scheme.error,
                )
        }
    }
}
