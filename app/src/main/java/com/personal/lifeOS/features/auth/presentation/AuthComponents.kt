@file:Suppress("MaxLineLength")

package com.personal.lifeOS.features.auth.presentation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.personal.lifeOS.R
import com.personal.lifeOS.core.ui.designsystem.AppCard
import com.personal.lifeOS.core.ui.designsystem.AppDesignTokens
import com.personal.lifeOS.core.ui.designsystem.HeroStatChip
import com.personal.lifeOS.core.ui.designsystem.HeroSurface

@Composable
internal fun AuthLoadingState() {
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(AppDesignTokens.spacing.sm),
        ) {
            Image(
                painter = painterResource(id = R.drawable.logo_personalos_mark),
                contentDescription = "PersonalOS",
                modifier =
                    Modifier
                        .size(88.dp)
                        .clip(RoundedCornerShape(AppDesignTokens.radius.lg)),
                contentScale = ContentScale.Crop,
            )
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 2.5.dp,
            )
        }
    }
}

@Composable
internal fun AuthBrandingHeader(isSignUpMode: Boolean) {
    HeroSurface(
        eyebrow = if (isSignUpMode) "Create account" else "Sign in",
        title = if (isSignUpMode) "Build your PersonalOS space" else "Welcome back",
        subtitle =
            if (isSignUpMode) {
                "Create your profile and continue into onboarding."
            } else {
                "Sign in to continue with your tasks, calendar, and finance flow."
            },
        action = {
            Box(
                modifier =
                    Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            brush =
                                Brush.linearGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.primary,
                                        AppDesignTokens.colors.primaryContainer,
                                    ),
                                ),
                        ),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(id = R.drawable.logo_personalos_mark),
                    contentDescription = "PersonalOS mark",
                    modifier = Modifier.size(24.dp),
                )
            }
        },
        footer = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                HeroStatChip(
                    label = "Security",
                    value = "Biometric-ready",
                    modifier = Modifier.weight(1f),
                )
                HeroStatChip(
                    label = "Sync",
                    value = "Cloud-safe",
                    modifier = Modifier.weight(1f),
                )
            }
        },
    )
}

@Composable
internal fun SignInCard(
    state: AuthUiState,
    viewModel: AuthViewModel,
) {
    AppCard(elevated = true) {
        Column(verticalArrangement = Arrangement.spacedBy(AppDesignTokens.spacing.md)) {
            AuthTextField(
                value = state.email,
                onValueChange = { viewModel.onEvent(AuthUiEvent.UpdateEmail(it)) },
                label = "Email Address",
                leadingIcon = Icons.Filled.Email,
                keyboardType = KeyboardType.Email,
                placeholder = "name@example.com",
            )
            AuthTextField(
                value = state.password,
                onValueChange = { viewModel.onEvent(AuthUiEvent.UpdatePassword(it)) },
                label = "Password",
                leadingIcon = Icons.Filled.Lock,
                isPassword = true,
                showPassword = state.showPassword,
                onTogglePassword = { viewModel.onEvent(AuthUiEvent.TogglePasswordVisibility) },
                placeholder = "Enter your password",
                trailingTextAction = "Forgot?",
                onTrailingTextAction = { viewModel.onEvent(AuthUiEvent.SendPasswordReset) },
            )
            Button(
                onClick = { viewModel.onEvent(AuthUiEvent.SignIn) },
                enabled = !state.isLoading,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                shape = RoundedCornerShape(AppDesignTokens.radius.pill),
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text("Sign In", fontWeight = FontWeight.SemiBold)
                }
            }
            AuthSwitcherLabel(
                prefix = "New here?",
                action = "Create Account",
                onClick = { viewModel.onEvent(AuthUiEvent.SwitchToSignUp) },
            )
        }
    }
}

@Composable
internal fun SignUpCard(
    state: AuthUiState,
    viewModel: AuthViewModel,
) {
    var acceptedTerms by remember { mutableStateOf(true) }
    AppCard(elevated = true) {
        Column(verticalArrangement = Arrangement.spacedBy(AppDesignTokens.spacing.md)) {
            AuthTextField(
                value = state.signUpUsername,
                onValueChange = { viewModel.onEvent(AuthUiEvent.UpdateSignUpUsername(it)) },
                label = "Full Name",
                leadingIcon = Icons.Filled.Person,
                placeholder = "Your name",
            )
            AuthTextField(
                value = state.signUpEmail,
                onValueChange = { viewModel.onEvent(AuthUiEvent.UpdateSignUpEmail(it)) },
                label = "Email Address",
                leadingIcon = Icons.Filled.Email,
                keyboardType = KeyboardType.Email,
                placeholder = "you@personal-os.com",
            )
            AuthTextField(
                value = state.signUpPassword,
                onValueChange = { viewModel.onEvent(AuthUiEvent.UpdateSignUpPassword(it)) },
                label = "Secure Password",
                leadingIcon = Icons.Filled.Lock,
                isPassword = true,
                showPassword = state.showPassword,
                onTogglePassword = { viewModel.onEvent(AuthUiEvent.TogglePasswordVisibility) },
                placeholder = "Create a secure password",
            )
            AuthTextField(
                value = state.signUpConfirmPassword,
                onValueChange = { viewModel.onEvent(AuthUiEvent.UpdateSignUpConfirmPassword(it)) },
                label = "Confirm Password",
                leadingIcon = Icons.Filled.Lock,
                isPassword = true,
                showPassword = state.showPassword,
                onTogglePassword = { viewModel.onEvent(AuthUiEvent.TogglePasswordVisibility) },
                placeholder = "Re-enter your password",
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = acceptedTerms,
                    onCheckedChange = { acceptedTerms = it },
                )
                Text(
                    text = "I agree to Terms and Privacy Policy.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Button(
                onClick = { viewModel.onEvent(AuthUiEvent.SignUp) },
                enabled = !state.isLoading && acceptedTerms,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                shape = RoundedCornerShape(AppDesignTokens.radius.pill),
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text("Get Started", fontWeight = FontWeight.SemiBold)
                }
            }
            AuthSwitcherLabel(
                prefix = "Already have an account?",
                action = "Sign In",
                onClick = { viewModel.onEvent(AuthUiEvent.SwitchToSignIn) },
            )
        }
    }
}

@Composable
private fun AuthSwitcherLabel(
    prefix: String,
    action: String,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = prefix,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = action,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.clickable(onClick = onClick),
        )
    }
}

@Composable
@Suppress("LongParameterList")
private fun AuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    leadingIcon: ImageVector,
    keyboardType: KeyboardType = KeyboardType.Text,
    placeholder: String,
    isPassword: Boolean = false,
    showPassword: Boolean = false,
    onTogglePassword: (() -> Unit)? = null,
    trailingTextAction: String? = null,
    onTrailingTextAction: (() -> Unit)? = null,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            if (!trailingTextAction.isNullOrBlank() && onTrailingTextAction != null) {
                Text(
                    text = trailingTextAction,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable(onClick = onTrailingTextAction),
                )
            }
        }

        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            leadingIcon = { Icon(leadingIcon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
            trailingIcon =
                if (isPassword) {
                    {
                        IconButton(onClick = { onTogglePassword?.invoke() }) {
                            Icon(
                                imageVector = if (showPassword) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                contentDescription = "Toggle password",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                } else {
                    null
                },
            placeholder = {
                Text(
                    text = placeholder,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                )
            },
            visualTransformation =
                if (isPassword && !showPassword) {
                    PasswordVisualTransformation()
                } else {
                    VisualTransformation.None
                },
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            shape = RoundedCornerShape(AppDesignTokens.radius.md),
            colors =
                OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                ),
        )
    }
}
