package com.personal.lifeOS.core.ui.designsystem

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
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
            glass -> MaterialTheme.colorScheme.surface.copy(alpha = 0.78f)
            elevated -> AppDesignTokens.colors.surfaceContainerLow
            else -> AppDesignTokens.colors.surfaceContainerLowest
        }
    val borderStroke: BorderStroke? =
        when {
            glass -> BorderStroke(1.dp, Color.White.copy(alpha = 0.18f))
            elevated -> null  // tonal fill expresses elevation; border is redundant
            else -> BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        }
    val elevation =
        when {
            glass -> AppDesignTokens.elevation.floating
            elevated -> AppDesignTokens.elevation.card * 2
            else -> 0.dp
        }
    val cardModifier =
        modifier
            .fillMaxWidth()
            .then(
                if (elevation > 0.dp) {
                    Modifier.shadow(
                        elevation = elevation,
                        shape = shape,
                        ambientColor = Color.Black.copy(alpha = 0.16f),
                        spotColor = Color.Black.copy(alpha = 0.16f),
                    )
                } else {
                    Modifier
                },
            )
            .clip(shape)
            .background(baseColor, shape)
            .then(
                if (borderStroke != null) {
                    Modifier.border(borderStroke, shape)
                } else {
                    Modifier
                },
            )
    Box(
        modifier =
            cardModifier,
    ) {
        if (glass) {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors =
                                    listOf(
                                        Color.White.copy(alpha = 0.08f),
                                        Color.White.copy(alpha = 0.02f),
                                        Color.Transparent,
                                    ),
                            ),
                        ),
            )
        }
        Box(modifier = Modifier.padding(contentPadding)) {
            content()
        }
    }
}
