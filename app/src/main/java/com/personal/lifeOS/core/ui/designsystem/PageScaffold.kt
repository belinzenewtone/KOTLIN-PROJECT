package com.personal.lifeOS.core.ui.designsystem

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.personal.lifeOS.ui.theme.AppSpacing

enum class PageHeaderVariant {
    HERO,
    COMPACT,
}

@Composable
fun PageScaffold(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    headerEyebrow: String? = null,
    headerVariant: PageHeaderVariant = PageHeaderVariant.HERO,
    topBanner: @Composable (() -> Unit)? = null,
    /** When non-null, a back arrow button is rendered before the title. */
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    contentPadding: PaddingValues = PaddingValues(bottom = AppSpacing.BottomSafe),
    content: @Composable () -> Unit,
) {
    val bgColor by animateColorAsState(
        targetValue = MaterialTheme.colorScheme.background,
        animationSpec = tween(durationMillis = 300),
        label = "scaffoldBg",
    )

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(bgColor)
                .statusBarsPadding(),
    ) {
        // Top banner - placed above everything else
        if (topBanner != null) {
            topBanner()
        }

        // Scrollable content area
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = AppSpacing.ScreenHorizontal)
                .padding(top = AppDesignTokens.spacing.sm)
                .padding(contentPadding),
            verticalArrangement = Arrangement.spacedBy(AppDesignTokens.spacing.lg),
        ) {
            when (headerVariant) {
                PageHeaderVariant.HERO -> {
                    HeroSurface(
                        eyebrow = headerEyebrow,
                        title = title,
                        subtitle = subtitle,
                        leading =
                            if (onBack != null) {
                                {
                                    IconButton(
                                        onClick = onBack,
                                        modifier =
                                            Modifier
                                                .size(36.dp)
                                                .clip(RoundedCornerShape(AppDesignTokens.radius.pill)),
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                            contentDescription = "Go back",
                                            tint = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.size(20.dp),
                                        )
                                    }
                                }
                            } else {
                                null
                            },
                        action = {
                            actions()
                        }
                    )
                }
                PageHeaderVariant.COMPACT -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top,
                    ) {
                        // Back button + title column — weight(1f) ensures actions are never pushed off-screen
                        Row(
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (onBack != null) {
                                IconButton(
                                    onClick = onBack,
                                    modifier = Modifier.size(36.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                        contentDescription = "Go back",
                                        tint = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(22.dp),
                                    )
                                }
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.headlineLarge,
                                    color = MaterialTheme.colorScheme.onBackground,
                                )
                                subtitle?.let {
                                    Text(
                                        text = it,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            actions()
                        }
                    }
                }
            }
            content()
        }
    }
}
