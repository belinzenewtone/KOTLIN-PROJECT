package com.personal.lifeOS.core.ui.designsystem

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    glass: Boolean = false,
    elevated: Boolean = false,
    contentPadding: PaddingValues = PaddingValues(20.dp),
    content: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(AppDesignTokens.radius.lg)
    // Surface hierarchy: surfaceContainerLowest < surface < surfaceContainerLow
    // Flat cards sit on surfaceContainerLowest so they lift off the background in both themes.
    // Elevated cards step up to surfaceContainerLow — tonal fill conveys elevation; no border needed.
    // Glass cards keep a translucent white border for the frosted effect.
    val baseColor =
        when {
            glass -> MaterialTheme.colorScheme.surface.copy(alpha = 0.72f)
            elevated -> AppDesignTokens.colors.surfaceContainerLow
            else -> AppDesignTokens.colors.surfaceContainerLowest
        }
    val borderStroke: BorderStroke? =
        when {
            glass -> BorderStroke(1.dp, Color.White.copy(alpha = 0.30f))
            elevated -> null  // tonal fill expresses elevation; border is redundant
            else -> BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        }
    val baseModifier = modifier
        .fillMaxWidth()
        .background(baseColor, shape)
    Box(
        modifier =
            (if (borderStroke != null) baseModifier.border(borderStroke, shape) else baseModifier)
                .padding(contentPadding),
    ) {
        content()
    }
}
