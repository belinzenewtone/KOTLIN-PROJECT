@file:Suppress("MaxLineLength")

package com.personal.lifeOS.features.profile.presentation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MarkEmailUnread
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.outlined.Email
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.personal.lifeOS.core.ui.designsystem.AppCard
import com.personal.lifeOS.core.ui.designsystem.AppDesignTokens
import com.personal.lifeOS.core.utils.DateUtils
import com.personal.lifeOS.features.auth.presentation.AuthUiState
import com.personal.lifeOS.features.profile.domain.model.UserProfile
import java.io.File

@Composable
internal fun ProfileIdentityCard(
    profile: UserProfile,
    onChangePhoto: () -> Unit,
) {
    AppCard(elevated = true) {
        Column(verticalArrangement = Arrangement.spacedBy(AppDesignTokens.spacing.sm)) {
            ProfileAvatar(
                profilePicPath = profile.profilePicUri,
                avatarInitials = profile.avatarInitials,
                onChangePhoto = onChangePhoto,
            )
            Text(
                text = profile.name.ifBlank { "Set up your profile" },
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
            if (profile.email.isNotBlank()) {
                Text(
                    text = profile.email,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text =
                    if (profile.memberSince > 0L) {
                        "Member since ${DateUtils.formatDate(profile.memberSince, "MMM dd, yyyy")}"
                    } else {
                        "Save your profile to set your member date."
                    },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ProfileAvatar(
    profilePicPath: String,
    avatarInitials: String,
    onChangePhoto: () -> Unit,
) {
    val hasPhoto = profilePicPath.isNotEmpty() && File(profilePicPath).exists()
    Box(contentAlignment = Alignment.BottomEnd) {
        Box(
            modifier =
                Modifier
                    .size(94.dp)
                    .clip(CircleShape)
                    .background(
                        brush =
                            if (hasPhoto) {
                                Brush.linearGradient(
                                    colors =
                                        listOf(
                                            MaterialTheme.colorScheme.surface,
                                            MaterialTheme.colorScheme.surface,
                                        ),
                                )
                            } else {
                                Brush.linearGradient(
                                    colors =
                                        listOf(
                                            MaterialTheme.colorScheme.primary,
                                            AppDesignTokens.colors.primaryContainer,
                                        ),
                                )
                            },
                    )
                    .clickable(onClick = onChangePhoto),
            contentAlignment = Alignment.Center,
        ) {
            if (hasPhoto) {
                Image(
                    painter = rememberAsyncImagePainter(model = File(profilePicPath)),
                    contentDescription = "Profile photo",
                    modifier = Modifier.size(94.dp),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Text(
                    text = avatarInitials,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        Box(
            modifier =
                Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .clickable(onClick = onChangePhoto),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.CameraAlt,
                contentDescription = "Change photo",
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Composable
internal fun EmailVerificationCard(
    authState: AuthUiState?,
    onResendVerification: () -> Unit,
) {
    if (authState == null) return
    val verified = authState.emailVerified
    AppCard(elevated = true) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable(enabled = !verified, onClick = onResendVerification),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppDesignTokens.spacing.sm),
        ) {
            Icon(
                imageVector = if (verified) Icons.Filled.Verified else Icons.Filled.MarkEmailUnread,
                contentDescription = null,
                tint = if (verified) AppDesignTokens.colors.success else AppDesignTokens.colors.warning,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (verified) "Account Verified" else "Email Verification Needed",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text =
                        if (verified) {
                            "Your email has been confirmed."
                        } else {
                            "Tap here to resend your verification link."
                        },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
internal fun ProfileDetailsCard(
    state: ProfileUiState,
    onEdit: () -> Unit,
) {
    AppCard(elevated = true) {
        Column(verticalArrangement = Arrangement.spacedBy(AppDesignTokens.spacing.sm)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Personal Information", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                IconButton(onClick = onEdit) {
                    Icon(Icons.Filled.Edit, contentDescription = "Edit profile", tint = MaterialTheme.colorScheme.primary)
                }
            }
            ProfileInfoRow(icon = Icons.Filled.Person, label = "Name", value = state.profile.name.ifBlank { "Not set" })
            ProfileInfoRow(icon = Icons.Outlined.Email, label = "Email", value = state.profile.email.ifBlank { "Not set" })
            ProfileInfoRow(icon = Icons.Outlined.Phone, label = "Phone", value = state.profile.phone.ifBlank { "Not set" })
        }
    }
}

@Composable
internal fun ProfileEditorCard(
    state: ProfileUiState,
    viewModel: ProfileViewModel,
) {
    AppCard(elevated = true) {
        Column(verticalArrangement = Arrangement.spacedBy(AppDesignTokens.spacing.sm)) {
            Text("Edit Profile", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            ProfileTextField(label = "Full Name", value = state.editName, onValueChange = viewModel::updateEditName)
            ProfileTextField(label = "Email", value = state.editEmail, onValueChange = viewModel::updateEditEmail)
            ProfileTextField(label = "Phone", value = state.editPhone, onValueChange = viewModel::updateEditPhone)
            Row(horizontalArrangement = Arrangement.spacedBy(AppDesignTokens.spacing.sm)) {
                OutlinedButton(onClick = viewModel::cancelEditing, modifier = Modifier.weight(1f)) {
                    Text("Cancel")
                }
                Button(onClick = viewModel::saveProfile, modifier = Modifier.weight(1f)) {
                    Text("Save")
                }
            }
        }
    }
}

@Composable
private fun ProfileTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        singleLine = true,
        colors =
            OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            ),
    )
}

@Composable
internal fun ProfileSecurityCard(
    biometricEnabled: Boolean,
    onChangePassword: () -> Unit,
    onToggleBiometric: (Boolean) -> Unit,
) {
    AppCard(elevated = true) {
        Column(verticalArrangement = Arrangement.spacedBy(AppDesignTokens.spacing.sm)) {
            Text("Security", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            ActionRow(icon = Icons.Filled.Lock, title = "Change Password", subtitle = "Update your local app password", onClick = onChangePassword)
            ToggleRow(icon = Icons.Filled.Fingerprint, title = "Biometric Lock", subtitle = "Require biometric unlock on resume", checked = biometricEnabled, onToggle = onToggleBiometric)
        }
    }
}

@Composable
internal fun ProfilePreferencesCard(
    notificationsEnabled: Boolean,
    onToggleNotifications: (Boolean) -> Unit,
) {
    AppCard(elevated = true) {
        Column(verticalArrangement = Arrangement.spacedBy(AppDesignTokens.spacing.sm)) {
            Text("Preferences", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            ToggleRow(icon = Icons.Filled.Notifications, title = "Notifications", subtitle = "Task and event reminders", checked = notificationsEnabled, onToggle = onToggleNotifications)
        }
    }
}

@Composable
internal fun ProfileCloudSyncCard(
    onBackup: () -> Unit,
    onRestore: () -> Unit,
) {
    AppCard(elevated = true) {
        Column(verticalArrangement = Arrangement.spacedBy(AppDesignTokens.spacing.sm)) {
            Text("Cloud Sync", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            ActionRow(icon = Icons.Filled.CloudUpload, title = "Backup to Cloud", subtitle = "Push local data to Supabase", onClick = onBackup)
            ActionRow(icon = Icons.Filled.CloudDownload, title = "Restore from Cloud", subtitle = "Pull cloud snapshot into local cache", onClick = onRestore)
        }
    }
}

@Composable
internal fun ProfileAppInfoCard() {
    AppCard(elevated = true) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("PersonalOS", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text("Version 1.0.0 • Master Your Plan", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
internal fun SignOutButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier =
            Modifier
                .fillMaxWidth()
                .height(52.dp),
        colors = ButtonDefaults.buttonColors(containerColor = AppDesignTokens.colors.error.copy(alpha = 0.12f), contentColor = AppDesignTokens.colors.error),
    ) {
        Icon(imageVector = Icons.AutoMirrored.Filled.Logout, contentDescription = null)
        Spacer(modifier = Modifier.size(8.dp))
        Text("Sign Out")
    }
}

@Composable
private fun ProfileInfoRow(
    icon: ImageVector,
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppDesignTokens.spacing.sm),
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun ActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surface)
                .clickable(onClick = onClick)
                .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppDesignTokens.spacing.sm),
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Column {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppDesignTokens.spacing.sm),
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onToggle)
    }
}

@Composable
internal fun ChangePasswordDialog(
    state: ProfileUiState,
    viewModel: ProfileViewModel,
) {
    AlertDialog(
        onDismissRequest = viewModel::hidePasswordDialog,
        title = { Text("Change Password") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PasswordField("Current Password", state.currentPassword, viewModel::updateCurrentPassword)
                PasswordField("New Password", state.newPassword, viewModel::updateNewPassword)
                PasswordField("Confirm Password", state.confirmPassword, viewModel::updateConfirmPassword)
                state.passwordError?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFB3261E),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = viewModel::changePassword) {
                Text("Update")
            }
        },
        dismissButton = {
            TextButton(onClick = viewModel::hidePasswordDialog) {
                Text("Cancel")
            }
        },
    )
}

@Composable
private fun PasswordField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        modifier = Modifier.fillMaxWidth(),
        colors =
            OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            ),
    )
}
