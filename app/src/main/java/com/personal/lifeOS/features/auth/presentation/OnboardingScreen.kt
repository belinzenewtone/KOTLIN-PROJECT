@file:Suppress("MaxLineLength")

package com.personal.lifeOS.features.auth.presentation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.personal.lifeOS.R
import com.personal.lifeOS.core.ui.designsystem.AppCard
import com.personal.lifeOS.core.ui.designsystem.AppDesignTokens
import com.personal.lifeOS.core.ui.designsystem.HeroSurface
import com.personal.lifeOS.core.ui.designsystem.InlineBanner
import com.personal.lifeOS.core.ui.designsystem.InlineBannerTone

@Composable
@Suppress("LongMethod")
fun OnboardingScreen(
    onFinished: () -> Unit,
    onBackToAuth: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(state.isCompleted) {
        if (state.isCompleted) {
            onFinished()
        }
    }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = AppDesignTokens.spacing.md, vertical = AppDesignTokens.spacing.sm),
        verticalArrangement = Arrangement.spacedBy(AppDesignTokens.spacing.md),
    ) {
        HeroSurface(
            eyebrow = "Step ${state.currentStep} of 4",
            title = "PersonalOS setup",
            subtitle = onboardingStepSubtitle(state.currentStep),
            action = {
                IconButton(
                    onClick = {
                        if (state.currentStep == 1) onBackToAuth() else viewModel.onEvent(OnboardingUiEvent.GoBack)
                    },
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            },
        )

        state.errorMessage?.let {
            InlineBanner(
                message = it,
                tone = InlineBannerTone.ERROR,
            )
        }

        AppCard(
            modifier = Modifier.fillMaxWidth(),
            elevated = true,
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(AppDesignTokens.spacing.lg),
            ) {
                when (state.currentStep) {
                    1 -> OnboardingWelcomeStep()
                    2 -> OnboardingPillarsStep()
                    3 ->
                        OnboardingProfileSetupStep(
                            fullName = state.fullName,
                            selectedGoal = state.primaryGoal,
                            onNameChange = { viewModel.onEvent(OnboardingUiEvent.UpdateFullName(it)) },
                            onGoalSelect = { viewModel.onEvent(OnboardingUiEvent.SelectGoal(it)) },
                        )
                    else -> OnboardingFinalStep()
                }
            }
        }

        Button(
            onClick = {
                if (state.currentStep == 4) {
                    viewModel.onEvent(OnboardingUiEvent.Complete)
                } else {
                    viewModel.onEvent(OnboardingUiEvent.Continue)
                }
            },
            enabled = !state.isSaving,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(56.dp),
            shape = RoundedCornerShape(AppDesignTokens.radius.pill),
        ) {
            if (state.isSaving) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            } else {
                Text(
                    text =
                        when (state.currentStep) {
                            1 -> "Let's Begin"
                            2 -> "Continue"
                            3 -> "Almost there"
                            else -> "Start My Journey"
                        },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }

        OnboardingProgressFooter(step = state.currentStep)
    }
}

private fun onboardingStepSubtitle(step: Int): String =
    when (step) {
        1 -> "A calm setup to personalize your planning and finance workspace."
        2 -> "Understand the core pillars that shape your daily flow."
        3 -> "Add your profile details so your workspace feels personal."
        else -> "Final checks before launching into your dashboard."
    }

@Composable
private fun OnboardingWelcomeStep() {
    Column(verticalArrangement = Arrangement.spacedBy(AppDesignTokens.spacing.md)) {
        Box(
            modifier =
                Modifier
                    .size(108.dp)
                    .clip(RoundedCornerShape(AppDesignTokens.radius.lg))
                    .background(MaterialTheme.colorScheme.surface),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(id = R.drawable.logo_personalos_mark),
                contentDescription = "PersonalOS logo",
                modifier = Modifier.size(68.dp),
            )
        }
        Text("Welcome to your PersonalOS", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        Text(
            "Your sanctuary for productivity, finance, and mindful planning.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun OnboardingPillarsStep() {
    Column(verticalArrangement = Arrangement.spacedBy(AppDesignTokens.spacing.md)) {
        Text("One place for everything.", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        Text(
            "PersonalOS keeps your planning and money flows aligned in one calm surface.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OnboardingPillarCard(icon = Icons.Filled.Speed, title = "Productivity", description = "Prioritize what matters and keep focused execution daily.")
        OnboardingPillarCard(icon = Icons.Filled.PieChart, title = "Finance", description = "Track spending, watch budgets, and review trends with confidence.")
    }
}

@Composable
private fun OnboardingPillarCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
) {
    AppCard(elevated = true) {
        Column(verticalArrangement = Arrangement.spacedBy(AppDesignTokens.spacing.xs)) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun OnboardingProfileSetupStep(
    fullName: String,
    selectedGoal: OnboardingGoal,
    onNameChange: (String) -> Unit,
    onGoalSelect: (OnboardingGoal) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(AppDesignTokens.spacing.md)) {
        Text("Tell us about yourself.", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        Text("This helps personalize your workspace.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedTextField(
            value = fullName,
            onValueChange = onNameChange,
            label = { Text("Full name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(AppDesignTokens.radius.md),
            colors =
                OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                ),
        )
        OnboardingGoal.entries.forEach { goal ->
            val selected = selectedGoal == goal
            AppCard(
                elevated = true,
                modifier =
                    Modifier
                        .clickable { onGoalSelect(goal) }
                        .clip(RoundedCornerShape(AppDesignTokens.radius.md)),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AppDesignTokens.spacing.sm),
                ) {
                    Icon(
                        imageVector = if (goal == OnboardingGoal.PRODUCTIVITY) Icons.Filled.RocketLaunch else Icons.Filled.PieChart,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(goal.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(goal.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (selected) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}

@Composable
private fun OnboardingFinalStep() {
    Column(verticalArrangement = Arrangement.spacedBy(AppDesignTokens.spacing.md)) {
        Text("You're all set.", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        Text("Welcome to your new digital sanctuary.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        OnboardingPillarCard(icon = Icons.Filled.AutoAwesome, title = "Personalized Insights", description = "Actionable summaries tuned to your real usage.")
        OnboardingPillarCard(icon = Icons.Filled.Speed, title = "Unified Workflow", description = "Tasks, calendar, and finance in a single rhythm.")
        OnboardingPillarCard(icon = Icons.Filled.Shield, title = "Private & Secure", description = "Your data stays controlled, with transparent protection.")
    }
}

@Composable
private fun OnboardingProgressFooter(step: Int) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AppDesignTokens.spacing.xs),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(4) { index ->
                Box(
                    modifier =
                        Modifier
                            .size(width = 28.dp, height = 4.dp)
                            .clip(CircleShape)
                            .background(
                                if (index < step) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.outlineVariant
                                },
                            ),
                )
            }
        }
        Text("Step $step of 4", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
