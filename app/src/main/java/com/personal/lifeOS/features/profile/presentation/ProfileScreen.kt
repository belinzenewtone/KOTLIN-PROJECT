package com.personal.lifeOS.features.profile.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.personal.lifeOS.ui.components.GlassCard
import com.personal.lifeOS.ui.theme.*

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        Text(
            "Profile",
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(24.dp))

        // Avatar
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(Primary, Accent)
                    )
                )
                .border(2.dp, Color.White.copy(alpha = 0.2f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = state.profile.avatarInitials.ifEmpty { "?" },
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = BackgroundDark
            )
        }

        Spacer(Modifier.height(12.dp))
        Text(
            state.profile.name.ifEmpty { "Set up your profile" },
            style = MaterialTheme.typography.headlineMedium
        )
        if (state.profile.email.isNotEmpty()) {
            Text(state.profile.email, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        }

        Spacer(Modifier.height(24.dp))

        // Edit Profile / View Profile Card
        if (state.isEditing) {
            EditProfileCard(state, viewModel)
        } else {
            ProfileInfoCard(state, viewModel)
        }

        Spacer(Modifier.height(16.dp))

        // Security Settings
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                Text("Security", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(16.dp))

                SettingsRow(
                    icon = Icons.Filled.Lock,
                    iconColor = Warning,
                    title = "Change Password",
                    subtitle = "Update your app password",
                    onClick = { viewModel.showPasswordDialog() }
                )

                Spacer(Modifier.height(12.dp))

                SettingsToggleRow(
                    icon = Icons.Filled.Fingerprint,
                    iconColor = Primary,
                    title = "Biometric Lock",
                    subtitle = "Use fingerprint to unlock",
                    checked = state.profile.isBiometricEnabled,
                    onToggle = { viewModel.toggleBiometric(it) }
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // Preferences
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                Text("Preferences", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(16.dp))

                SettingsToggleRow(
                    icon = Icons.Filled.Notifications,
                    iconColor = Accent,
                    title = "Notifications",
                    subtitle = "Event and task reminders",
                    checked = state.profile.notificationsEnabled,
                    onToggle = { viewModel.toggleNotifications(it) }
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // App info
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Info, null, tint = TextTertiary, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("LifeOS", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                    Text("Version 1.0.0 • Personal Life Operating System", style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        Spacer(Modifier.height(100.dp)) // space for floating nav bar
    }

    // Password dialog
    if (state.showPasswordDialog) {
        ChangePasswordDialog(state, viewModel)
    }
}

@Composable
private fun ProfileInfoCard(state: ProfileUiState, viewModel: ProfileViewModel) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Personal Info", style = MaterialTheme.typography.titleMedium)
                IconButton(onClick = { viewModel.startEditing() }) {
                    Icon(Icons.Filled.Edit, "Edit", tint = Primary)
                }
            }

            Spacer(Modifier.height(8.dp))

            InfoRow(Icons.Filled.Person, "Name", state.profile.name.ifEmpty { "Not set" })
            Spacer(Modifier.height(12.dp))
            InfoRow(Icons.Outlined.Email, "Email", state.profile.email.ifEmpty { "Not set" })
            Spacer(Modifier.height(12.dp))
            InfoRow(Icons.Outlined.Phone, "Phone", state.profile.phone.ifEmpty { "Not set" })
        }
    }
}

@Composable
private fun InfoRow(icon: ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Primary.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = Primary, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall, color = TextTertiary)
            Text(value, style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
        }
    }
}

@Composable
private fun EditProfileCard(state: ProfileUiState, viewModel: ProfileViewModel) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Text("Edit Profile", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(16.dp))

            GlassTextField("Full Name", state.editName) { viewModel.updateEditName(it) }
            Spacer(Modifier.height(12.dp))
            GlassTextField("Email", state.editEmail) { viewModel.updateEditEmail(it) }
            Spacer(Modifier.height(12.dp))
            GlassTextField("Phone", state.editPhone) { viewModel.updateEditPhone(it) }
            Spacer(Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { viewModel.cancelEditing() },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary)
                ) {
                    Text("Cancel")
                }
                Button(
                    onClick = { viewModel.saveProfile() },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Primary, contentColor = BackgroundDark)
                ) {
                    Text("Save", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun GlassTextField(label: String, value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Primary,
            unfocusedBorderColor = GlassBorder,
            focusedContainerColor = GlassWhite,
            unfocusedContainerColor = GlassWhite,
            cursorColor = Primary,
            focusedLabelColor = Primary,
            unfocusedLabelColor = TextTertiary
        )
    )
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(GlassWhite)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(iconColor.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = iconColor, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = TextTertiary)
        }
        IconButton(onClick = onClick) {
            Icon(Icons.Outlined.ChevronRight, null, tint = TextTertiary)
        }
    }
}

@Composable
private fun SettingsToggleRow(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    subtitle: String,
    checked: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(GlassWhite)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(iconColor.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = iconColor, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = TextTertiary)
        }
        Switch(
            checked = checked,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor = BackgroundDark,
                checkedTrackColor = Primary,
                uncheckedThumbColor = TextTertiary,
                uncheckedTrackColor = GlassWhite
            )
        )
    }
}

@Composable
private fun ChangePasswordDialog(state: ProfileUiState, viewModel: ProfileViewModel) {
    AlertDialog(
        onDismissRequest = { viewModel.hidePasswordDialog() },
        containerColor = SurfaceDark,
        shape = RoundedCornerShape(24.dp),
        title = { Text("Change Password", color = TextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = state.currentPassword,
                    onValueChange = { viewModel.updateCurrentPassword(it) },
                    label = { Text("Current Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Primary,
                        unfocusedBorderColor = GlassBorder,
                        focusedContainerColor = GlassWhite,
                        unfocusedContainerColor = GlassWhite,
                        cursorColor = Primary
                    )
                )
                OutlinedTextField(
                    value = state.newPassword,
                    onValueChange = { viewModel.updateNewPassword(it) },
                    label = { Text("New Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Primary,
                        unfocusedBorderColor = GlassBorder,
                        focusedContainerColor = GlassWhite,
                        unfocusedContainerColor = GlassWhite,
                        cursorColor = Primary
                    )
                )
                OutlinedTextField(
                    value = state.confirmPassword,
                    onValueChange = { viewModel.updateConfirmPassword(it) },
                    label = { Text("Confirm Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Primary,
                        unfocusedBorderColor = GlassBorder,
                        focusedContainerColor = GlassWhite,
                        unfocusedContainerColor = GlassWhite,
                        cursorColor = Primary
                    )
                )
                if (state.passwordError != null) {
                    Text(state.passwordError, color = Error, style = MaterialTheme.typography.labelSmall)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { viewModel.changePassword() }) {
                Text("Update", color = Primary, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = { viewModel.hidePasswordDialog() }) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}
