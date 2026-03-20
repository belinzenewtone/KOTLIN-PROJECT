package com.personal.lifeOS.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Reusable Glass Morphism card component.
 *
 * Uses the theme surface color as a base so the card is always readable in both
 * light and dark mode. A subtle white-alpha gradient adds the glass sheen on top.
 *
 * @param modifier Standard compose modifier
 * @param cornerRadius Corner radius (default 20dp per spec)
 * @param glassAlpha Additional white-sheen transparency (0f-1f)
 * @param elevation Shadow elevation
 * @param content Composable content inside the card
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 20.dp,
    glassAlpha: Float = 0.08f,
    @Suppress("UNUSED_PARAMETER") borderAlpha: Float = 0.10f, // kept for API compat
    elevation: Dp = 4.dp,
    content: @Composable BoxScope.() -> Unit,
) {
    val shape = remember(cornerRadius) { RoundedCornerShape(cornerRadius) }
    val surfaceColor = MaterialTheme.colorScheme.surface
    val outlineVariant = MaterialTheme.colorScheme.outlineVariant

    Box(
        modifier =
            modifier
                .shadow(elevation = elevation, shape = shape)
                .clip(shape)
                // 1. Solid surface base — guarantees legibility in both light & dark
                .background(surfaceColor)
                // 2. Subtle white-sheen gradient for the "glass" feel
                .background(
                    brush =
                        Brush.verticalGradient(
                            colors =
                                listOf(
                                    Color.White.copy(alpha = glassAlpha * 1.2f),
                                    Color.White.copy(alpha = glassAlpha * 0.4f),
                                ),
                        ),
                )
                .border(
                    width = 0.5.dp,
                    color = outlineVariant,
                    shape = shape,
                )
                .padding(16.dp),
        content = content,
    )
}

/**
 * Variant with accent-colored glow for highlighted cards.
 */
@Composable
fun AccentGlassCard(
    modifier: Modifier = Modifier,
    accentColor: Color = Color(0xFF2979FF),
    cornerRadius: Dp = 20.dp,
    content: @Composable BoxScope.() -> Unit,
) {
    val shape = RoundedCornerShape(cornerRadius)
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer

    Box(
        modifier =
            modifier
                .shadow(
                    elevation = 12.dp,
                    shape = shape,
                    ambientColor = accentColor.copy(alpha = 0.2f),
                    spotColor = accentColor.copy(alpha = 0.15f),
                )
                .clip(shape)
                // Solid primaryContainer base for legibility in both modes
                .background(primaryContainer)
                .background(
                    brush =
                        Brush.verticalGradient(
                            colors =
                                listOf(
                                    accentColor.copy(alpha = 0.10f),
                                    accentColor.copy(alpha = 0.02f),
                                ),
                        ),
                )
                .border(
                    width = 1.dp,
                    brush =
                        Brush.verticalGradient(
                            colors =
                                listOf(
                                    accentColor.copy(alpha = 0.3f),
                                    accentColor.copy(alpha = 0.05f),
                                ),
                        ),
                    shape = shape,
                )
                .padding(16.dp),
        content = content,
    )
}
