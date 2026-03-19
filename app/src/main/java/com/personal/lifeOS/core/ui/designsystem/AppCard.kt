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
    val baseColor =
        when {
            glass -> MaterialTheme.colorScheme.surface.copy(alpha = 0.72f)
            elevated -> AppDesignTokens.colors.surfaceContainerLowest
            else -> MaterialTheme.colorScheme.surface
        }
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .background(baseColor, shape)
                .border(
                    // Ghost border to preserve soft edges without hard separators.
                    border =
                        BorderStroke(
                            1.dp,
                            Color.White.copy(alpha = if (glass) 0.30f else 0.14f),
                        ),
                    shape = shape,
                )
                .padding(contentPadding),
    ) {
        content()
    }
}
