package com.personal.lifeOS.core.ui.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

@Composable
fun InlineBanner(
    message: String,
    modifier: Modifier = Modifier,
    tone: InlineBannerTone = InlineBannerTone.INFO,
) {
    val (bg, fg) =
        when (tone) {
            InlineBannerTone.INFO -> Color(0xFFE3F2FD) to Color(0xFF1565C0)
            InlineBannerTone.SUCCESS -> Color(0xFFE8F5E9) to Color(0xFF2E7D32)
            InlineBannerTone.WARNING -> Color(0xFFFFF8E1) to Color(0xFFEF6C00)
            InlineBannerTone.ERROR -> Color(0xFFFFEBEE) to Color(0xFFC62828)
        }

    Row(
        modifier =
            modifier
                .background(bg, RoundedCornerShape(AppDesignTokens.radius.md))
                .padding(horizontal = AppDesignTokens.spacing.md, vertical = AppDesignTokens.spacing.sm),
        horizontalArrangement = Arrangement.spacedBy(AppDesignTokens.spacing.xs),
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = fg,
        )
    }
}
