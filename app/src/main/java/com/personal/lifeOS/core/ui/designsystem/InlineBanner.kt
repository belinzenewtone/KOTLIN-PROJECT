package com.personal.lifeOS.core.ui.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@Composable
fun InlineBanner(
    message: String,
    modifier: Modifier = Modifier,
    tone: InlineBannerTone = InlineBannerTone.INFO,
) {
    val semanticTone =
        when (tone) {
            InlineBannerTone.INFO -> AppSemanticTone.INFO
            InlineBannerTone.SUCCESS -> AppSemanticTone.SUCCESS
            InlineBannerTone.WARNING -> AppSemanticTone.WARNING
            InlineBannerTone.ERROR -> AppSemanticTone.ERROR
        }
    val semanticColors = AppDesignTokens.semanticColors(semanticTone)
    val icon =
        when (tone) {
            InlineBannerTone.INFO -> Icons.Filled.Info as ImageVector
            InlineBannerTone.SUCCESS -> Icons.Filled.CheckCircle as ImageVector
            InlineBannerTone.WARNING -> Icons.Filled.Warning as ImageVector
            InlineBannerTone.ERROR -> Icons.Filled.ErrorOutline as ImageVector
        }

    Row(
        modifier =
            modifier
                .background(semanticColors.container, RoundedCornerShape(AppDesignTokens.radius.md))
                .padding(horizontal = AppDesignTokens.spacing.md, vertical = AppDesignTokens.spacing.sm),
        horizontalArrangement = Arrangement.spacedBy(AppDesignTokens.spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = semanticColors.icon,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = semanticColors.onContainer,
        )
    }
}
