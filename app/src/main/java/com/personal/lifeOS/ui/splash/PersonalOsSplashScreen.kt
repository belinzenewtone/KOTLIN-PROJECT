package com.personal.lifeOS.ui.splash

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.personal.lifeOS.R

@Composable
@Suppress("LongMethod")
fun PersonalOsSplashScreen(modifier: Modifier = Modifier) {
    var animateIn by remember { mutableStateOf(false) }
    val infiniteTransition = rememberInfiniteTransition(label = "splashPulse")
    val glowScale by
        infiniteTransition.animateFloat(
            initialValue = 0.92f,
            targetValue = 1.08f,
            animationSpec = infiniteRepeatable(animation = tween(1700), repeatMode = RepeatMode.Reverse),
            label = "glowScale",
        )

    val logoAlpha by
        animateFloatAsState(
            targetValue = if (animateIn) 1f else 0f,
            animationSpec = tween(durationMillis = 900, delayMillis = 100),
            label = "logoAlpha",
        )
    val logoScale by
        animateFloatAsState(
            targetValue = if (animateIn) 1f else 0.88f,
            animationSpec = spring(dampingRatio = 0.78f, stiffness = 240f),
            label = "logoScale",
        )
    val subtitleAlpha by
        animateFloatAsState(
            targetValue = if (animateIn) 1f else 0f,
            animationSpec = tween(durationMillis = 700, delayMillis = 500),
            label = "subtitleAlpha",
        )

    LaunchedEffect(Unit) { animateIn = true }

    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(
                    brush =
                        Brush.verticalGradient(
                            colors =
                                listOf(
                                    MaterialTheme.colorScheme.background,
                                    MaterialTheme.colorScheme.background.copy(alpha = 0.96f),
                                ),
                        ),
                ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Box(
                    modifier =
                        Modifier
                            .size(220.dp)
                            .graphicsLayer(
                                scaleX = glowScale,
                                scaleY = glowScale,
                                alpha = 0.35f,
                            )
                            .blur(28.dp)
                            .background(
                                brush =
                                    Brush.radialGradient(
                                        colors = listOf(MaterialTheme.colorScheme.primary.copy(alpha = 0.35f), MaterialTheme.colorScheme.primary.copy(alpha = 0f)),
                                    ),
                            ),
                )
                Image(
                    painter = painterResource(id = R.drawable.logo_personalos),
                    contentDescription = "PersonalOS logo",
                    modifier =
                        Modifier
                            .fillMaxWidth(0.74f)
                            .graphicsLayer(
                                alpha = logoAlpha,
                                scaleX = logoScale,
                                scaleY = logoScale,
                            ),
                )
            }

            Text(
                text = "Master your plan",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = subtitleAlpha),
            )

            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
                strokeWidth = 2.5.dp,
                modifier = Modifier.width(28.dp),
            )
        }
    }
}
