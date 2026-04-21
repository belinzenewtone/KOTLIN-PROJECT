@file:Suppress("MaxLineLength")

package com.personal.lifeOS.features.auth.presentation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.PieChart
import androidx.compose.material.icons.outlined.RocketLaunch
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
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
                .background(
                    brush =
                        Brush.verticalGradient(
                            colors =
                                listOf(
                                    MaterialTheme.colorScheme.background,
                                    MaterialTheme.colorScheme.surfaceContainerLowest,
                                ),
                        ),
                )
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = AppDesignTokens.spacing.md, vertical = AppDesignTokens.spacing.sm),
        verticalArrangement = Arrangement.spacedBy(AppDesignTokens.spacing.md),
    ) {
        // ── Header ─────────────────────────────────────────────────────────────
        // Back arrow is hidden on Step 1 — system back gesture handles auth exit.
        // Step number shown here only; the bottom footer shows progress dots only.
        HeroSurface(
            eyebrow = "Step ${state.currentStep} of 4",
            title = "PersonalOS setup",
            subtitle = onboardingStepSubtitle(state.currentStep),
            leading = {
                if (state.currentStep > 1) {
                    IconButton(
                        onClick = { viewModel.onEvent(OnboardingUiEvent.GoBack) },
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                } else {
                    // Placeholder keeps header layout balanced on Step 1
                    Box(Modifier.size(48.dp))
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

        // ── CTA button ─────────────────────────────────────────────────────────
        // Labels are consistently action-oriented across all steps.
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
                            4 -> "Start My Journey"
                            else -> "Continue"
                        },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }

        // ── Progress dots only — step number is already in the header above ───
        OnboardingProgressDots(step = state.currentStep)
    }
}

private fun onboardingStepSubtitle(step: Int): String =
    when (step) {
        1 -> "A calm setup to personalize your planning and finance workspace."
        2 -> "Understand the core pillars that shape your daily flow."
        3 -> "Tell us your name and what you want to focus on."
        else -> "Final checks before launching into your dashboard."
    }

// ── Step 1: Welcome ────────────────────────────────────────────────────────────
// Three compact feature rows fill the dead space below the headline.

@Composable
private fun OnboardingWelcomeStep() {
    Column(verticalArrangement = Arrangement.spacedBy(AppDesignTokens.spacing.md)) {
        Box(
            modifier =
                Modifier
                    .size(108.dp)
                    .clip(RoundedCornerShape(AppDesignTokens.radius.lg))
                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f),
                        shape = RoundedCornerShape(AppDesignTokens.radius.lg),
                    ),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(id = R.drawable.logo_personalos_mark),
                contentDescription = "PersonalOS logo",
                modifier = Modifier.size(68.dp),
            )
        }
        Text(
            "Welcome to your PersonalOS",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
        )
        Text(
            "Your sanctuary for productivity, finance, and mindful planning.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(AppDesignTokens.spacing.xs))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
        Spacer(Modifier.height(AppDesignTokens.spacing.xs))

        OnboardingFeatureRow(Icons.Outlined.Speed, "Productivity — tasks, routines, and focused planning")
        OnboardingFeatureRow(Icons.Outlined.PieChart, "Finance — budgets, spending, and trends at a glance")
        OnboardingFeatureRow(Icons.Outlined.CalendarMonth, "Calendar — events, birthdays, and smart reminders")
    }
}

@Composable
private fun OnboardingFeatureRow(icon: ImageVector, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppDesignTokens.spacing.sm),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// ── Step 2: Pillars ────────────────────────────────────────────────────────────
// Calendar added as the third pillar alongside Productivity and Finance.

@Composable
private fun OnboardingPillarsStep() {
    Column(verticalArrangement = Arrangement.spacedBy(AppDesignTokens.spacing.md)) {
        Text(
            "One place for everything.",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
        )
        Text(
            "PersonalOS keeps your planning and money flows aligned in one calm surface.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OnboardingPillarCard(
            icon = Icons.Outlined.Speed,
            title = "Productivity",
            description = "Prioritize what matters and keep focused execution daily.",
        )
        OnboardingPillarCard(
            icon = Icons.Outlined.CalendarMonth,
            title = "Planning & Calendar",
            description = "Events, reminders, birthdays, and countdowns — all in one view.",
        )
        OnboardingPillarCard(
            icon = Icons.Outlined.PieChart,
            title = "Finance",
            description = "Track spending, watch budgets, and review trends with confidence.",
        )
    }
}

@Composable
private fun OnboardingPillarCard(
    icon: ImageVector,
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

// ── Step 3: Profile setup ──────────────────────────────────────────────────────
// Section labels added above name field and goal list.
// Selected goal card shows a coloured border + subtle tinted background.

@Composable
private fun OnboardingProfileSetupStep(
    fullName: String,
    selectedGoal: OnboardingGoal,
    onNameChange: (String) -> Unit,
    onGoalSelect: (OnboardingGoal) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(AppDesignTokens.spacing.md)) {
        Text(
            "Tell us about yourself.",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
        )
        Text(
            "This helps personalize your workspace.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        // ── Name section ──────────────────────────────────────────────────────
        Column(verticalArrangement = Arrangement.spacedBy(AppDesignTokens.spacing.xs)) {
            Text(
                text = "Your name",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                value = fullName,
                onValueChange = onNameChange,
                placeholder = { Text("Full name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(AppDesignTokens.radius.md),
                colors =
                    OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.38f),
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.48f),
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                    ),
            )
        }

        // ── Goal section ──────────────────────────────────────────────────────
        Column(verticalArrangement = Arrangement.spacedBy(AppDesignTokens.spacing.sm)) {
            Text(
                text = "Your primary focus",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OnboardingGoal.entries.forEach { goal ->
                val selected = selectedGoal == goal
                val goalIcon = when (goal) {
                    OnboardingGoal.PRODUCTIVITY -> Icons.Outlined.RocketLaunch
                    OnboardingGoal.FINANCE -> Icons.Outlined.PieChart
                    OnboardingGoal.BALANCED -> Icons.Outlined.Tune
                }
                // Border + background change on selection for clear visual feedback
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(AppDesignTokens.radius.md))
                            .border(
                                width = if (selected) 1.5.dp else 0.5.dp,
                                color =
                                    if (selected) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                                    },
                                shape = RoundedCornerShape(AppDesignTokens.radius.md),
                            )
                            .background(
                                color =
                                    if (selected) {
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.07f)
                                    } else {
                                        MaterialTheme.colorScheme.surfaceContainerLow
                                    },
                            )
                            .clickable { onGoalSelect(goal) }
                            .padding(AppDesignTokens.spacing.md),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(AppDesignTokens.spacing.sm),
                    ) {
                        Icon(
                            imageVector = goalIcon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                goal.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                goal.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (selected) {
                            Icon(
                                Icons.Outlined.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── Step 4: All set ────────────────────────────────────────────────────────────

@Composable
private fun OnboardingFinalStep() {
    Column(verticalArrangement = Arrangement.spacedBy(AppDesignTokens.spacing.md)) {
        Text(
            "You're all set.",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
        )
        Text(
            "Welcome to your new digital sanctuary.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OnboardingPillarCard(
            icon = Icons.Outlined.AutoAwesome,
            title = "Personalized Insights",
            description = "Actionable summaries tuned to your real usage.",
        )
        OnboardingPillarCard(
            icon = Icons.Outlined.Speed,
            title = "Unified Workflow",
            description = "Tasks, calendar, and finance in a single rhythm.",
        )
        OnboardingPillarCard(
            icon = Icons.Outlined.Shield,
            title = "Private & Secure",
            description = "Your data stays controlled, with transparent protection.",
        )
    }
}

// ── Progress dots ──────────────────────────────────────────────────────────────
// "Step X of 4" text removed — it is already shown in the header above.

@Composable
private fun OnboardingProgressDots(step: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
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
    }
}
