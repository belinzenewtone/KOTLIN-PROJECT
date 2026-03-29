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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@Composable
fun InlineBanner(
    message: String,
    modifier: Modifier = Modifier,
    tone: InlineBannerTone = InlineBannerTone.INFO,
) {
    val (bg, fg, icon) =
        when (tone) {
            InlineBannerTone.INFO ->
                Triple(
                    MaterialTheme.colorScheme.primaryContainer,
                    MaterialTheme.colorScheme.onPrimaryContainer,
                    Icons.Filled.Info as ImageVector,
                )
            InlineBannerTone.SUCCESS ->
                Triple(
                    Color(0xFFE8F5E9),
                    Color(0xFF2E7D32),
                    Icons.Filled.CheckCircle as ImageVector,
                )
            InlineBannerTone.WARNING ->
                Triple(
                    Color(0xFFFFF8E1),
                    Color(0xFFEF6C00),
                    Icons.Filled.Warning as ImageVector,
                )
            InlineBannerTone.ERROR ->
                Triple(
                    Color(0xFFFFEBEE),
                    Color(0xFFC62828),
                    Icons.Filled.ErrorOutline as ImageVector,
                )
        }

    Row(
        modifier =
            modifier
                .background(bg, RoundedCornerShape(AppDesignTokens.radius.md))
                .padding(horizontal = AppDesignTokens.spacing.md, vertical = AppDesignTokens.spacing.sm),
        horizontalArrangement = Arrangement.spacedBy(AppDesignTokens.spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = fg,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = fg,
        )
    }
}

enum class InlineBannerTone { INFO, SUCCESS, WARNING, ERROR }
