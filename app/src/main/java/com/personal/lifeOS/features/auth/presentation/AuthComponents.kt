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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.personal.lifeOS.R
import com.personal.lifeOS.ui.components.GlassCard
import com.personal.lifeOS.ui.theme.BackgroundDark
import com.personal.lifeOS.ui.theme.GlassBorder
import com.personal.lifeOS.ui.theme.GlassWhite
import com.personal.lifeOS.ui.theme.Primary
import com.personal.lifeOS.ui.theme.TextSecondary
import com.personal.lifeOS.ui.theme.TextTertiary

@Composable
internal fun AuthLoadingState() {
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(BackgroundDark),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painter = painterResource(id = R.drawable.logo),
                contentDescription = "BELTECH",
                modifier =
                    Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(16.dp)),
                contentScale = ContentScale.Crop,
            )
            Spacer(Modifier.height(16.dp))
            CircularProgressIndicator(color = Primary, strokeWidth = 3.dp)
        }
    }
}

@Composable
internal fun AuthBrandingHeader() {
    Image(
        painter = painterResource(id = R.drawable.logo),
        contentDescription = "BELTECH",
        modifier =
            Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(16.dp)),
        contentScale = ContentScale.Crop,
    )
    Spacer(Modifier.height(16.dp))
    Text(
        text = "BELTECH",
        style = MaterialTheme.typography.headlineLarge,
        fontWeight = FontWeight.Bold,
    )
    Text(
        text = "Innovate and Create",
        style = MaterialTheme.typography.bodyMedium,
        color = TextTertiary,
    )
}

@Composable
internal fun SignInCard(
    state: AuthUiState,
    viewModel: AuthViewModel,
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Welcome Back", style = MaterialTheme.typography.headlineMedium)
            Text(
                "Sign in to your account",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
            )

            AuthTextField(
                value = state.email,
                onValueChange = { viewModel.updateEmail(it) },
                label = "Email",
                leadingIcon = Icons.Filled.Email,
                keyboardType = KeyboardType.Email,
            )

            AuthTextField(
                value = state.password,
                onValueChange = { viewModel.updatePassword(it) },
                label = "Password",
                leadingIcon = Icons.Filled.Lock,
                isPassword = true,
                showPassword = state.showPassword,
                onTogglePassword = { viewModel.togglePasswordVisibility() },
            )

            Button(
                onClick = { viewModel.signIn() },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = Primary,
                        contentColor = BackgroundDark,
                    ),
                enabled = !state.isLoading,
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(
                        color = BackgroundDark,
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text("Sign In", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
            ) {
                Text(
                    "Don't have an account? ",
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    "Sign Up",
                    color = Primary,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.clickable { viewModel.switchToSignUp() },
                )
            }
        }
    }
}

@Composable
internal fun SignUpCard(
    state: AuthUiState,
    viewModel: AuthViewModel,
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("Create Account", style = MaterialTheme.typography.headlineMedium)
            Text("Join BELTECH today", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)

            AuthTextField(
                value = state.signUpUsername,
                onValueChange = { viewModel.updateSignUpUsername(it) },
                label = "Username",
                leadingIcon = Icons.Filled.Person,
            )

            AuthTextField(
                value = state.signUpEmail,
                onValueChange = { viewModel.updateSignUpEmail(it) },
                label = "Email",
                leadingIcon = Icons.Filled.Email,
                keyboardType = KeyboardType.Email,
            )

            AuthTextField(
                value = state.signUpPassword,
                onValueChange = { viewModel.updateSignUpPassword(it) },
                label = "Password",
                leadingIcon = Icons.Filled.Lock,
                isPassword = true,
                showPassword = state.showPassword,
                onTogglePassword = { viewModel.togglePasswordVisibility() },
            )

            AuthTextField(
                value = state.signUpConfirmPassword,
                onValueChange = { viewModel.updateSignUpConfirmPassword(it) },
                label = "Confirm Password",
                leadingIcon = Icons.Filled.Lock,
                isPassword = true,
                showPassword = state.showPassword,
                onTogglePassword = { viewModel.togglePasswordVisibility() },
            )

            Button(
                onClick = { viewModel.signUp() },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = Primary,
                        contentColor = BackgroundDark,
                    ),
                enabled = !state.isLoading,
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(
                        color = BackgroundDark,
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text("Create Account", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
            ) {
                Text(
                    "Already have an account? ",
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    "Sign In",
                    color = Primary,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.clickable { viewModel.switchToSignIn() },
                )
            }
        }
    }
}

@Composable
private fun AuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    leadingIcon: ImageVector,
    keyboardType: KeyboardType = KeyboardType.Text,
    isPassword: Boolean = false,
    showPassword: Boolean = false,
    onTogglePassword: (() -> Unit)? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        leadingIcon = { Icon(leadingIcon, null, tint = TextTertiary) },
        trailingIcon =
            if (isPassword) {
                {
                    IconButton(onClick = { onTogglePassword?.invoke() }) {
                        Icon(
                            imageVector = if (showPassword) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                            contentDescription = null,
                            tint = TextTertiary,
                        )
                    }
                }
            } else {
                null
            },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        visualTransformation =
            if (isPassword && !showPassword) {
                PasswordVisualTransformation()
            } else {
                VisualTransformation.None
            },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        colors =
            OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Primary,
                unfocusedBorderColor = GlassBorder,
                focusedContainerColor = GlassWhite,
                unfocusedContainerColor = GlassWhite,
                cursorColor = Primary,
                focusedLabelColor = Primary,
                unfocusedLabelColor = TextTertiary,
            ),
    )
}
