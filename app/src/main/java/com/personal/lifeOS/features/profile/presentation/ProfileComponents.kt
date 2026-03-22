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
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MarkEmailUnread
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SettingsBrightness
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.WbSunny
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.rememberAsyncImagePainter
import com.personal.lifeOS.core.ui.designsystem.AppCard
import com.personal.lifeOS.core.ui.designsystem.AppDesignTokens
import com.personal.lifeOS.core.utils.DateUtils
import com.personal.lifeOS.features.auth.presentation.AuthUiState
import com.personal.lifeOS.features.profile.domain.model.UserProfile
import com.personal.lifeOS.ui.theme.AppThemeMode
import java.io.File

@Composable
internal fun ProfileIdentityCard(
    profile: UserProfile,
    authState: AuthUiState?,
    onChangePhoto: () -> Unit,
) {
    AppCard(elevated = true) {
        Column(verticalArrangement = Arrangement.spacedBy(AppDesignTokens.spacing.sm)) {
            ProfileAvatar(
                profilePicPath = profile.profilePicUri,
                avatarInitials = profile.avatarInitials,
                onChangePhoto = onChangePhoto,
                memberSince = profile.memberSince,
                emailVerified = authState?.emailVerified,
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
        }
    }
}

@Composable
private fun ProfileAvatar(
    profilePicPath: String,
    avatarInitials: String,
    memberSince: Long,
    emailVerified: Boolean?,
    onChangePhoto: () -> Unit,
) {
    val hasPhoto = profilePicPath.isNotEmpty() && File(profilePicPath).exists()
    var showPhotoDialog by remember { mutableStateOf(false) }

    // Full-screen photo preview dialog
    if (showPhotoDialog && hasPhoto) {
        Dialog(
            onDismissRequest = { showPhotoDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.85f))
                    .clickable { showPhotoDialog = false },
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = rememberAsyncImagePainter(model = File(profilePicPath)),
                    contentDescription = "Profile photo",
                    modifier = Modifier
                        .size(280.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop,
                )
            }
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
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
                                        colors = listOf(
                                            MaterialTheme.colorScheme.surface,
                                            MaterialTheme.colorScheme.surface,
                                        ),
                                    )
                                } else {
                                    Brush.linearGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.primary,
                                            AppDesignTokens.colors.primaryContainer,
                                        ),
                                    )
                                },
                        )
                        // Clicking avatar opens view-only dialog (only when there's a photo)
                        .clickable(enabled = hasPhoto) { showPhotoDialog = true },
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

            // Camera icon — this is the only way to change the photo
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

        // Member since label
        if (memberSince > 0L) {
            Text(
                text = "Member since ${DateUtils.formatDate(memberSince, "MMM dd, yyyy")}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // Email verification pill — lives inside the avatar section
        if (emailVerified != null) {
            val verified = emailVerified
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50.dp))
                    .background(
                        if (verified) AppDesignTokens.colors.success.copy(alpha = 0.15f)
                        else AppDesignTokens.colors.warning.copy(alpha = 0.15f),
                    )
                    .padding(horizontal = 12.dp, vertical = 4.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(
                        imageVector = if (verified) Icons.Filled.Verified else Icons.Filled.MarkEmailUnread,
                        contentDescription = null,
                        tint = if (verified) AppDesignTokens.colors.success else AppDesignTokens.colors.warning,
                        modifier = Modifier.size(12.dp),
                    )
                    Text(
                        text = if (verified) "Verified" else "Pending Verification",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 11.sp,
                        color = if (verified) AppDesignTokens.colors.success else AppDesignTokens.colors.warning,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

// EmailVerificationCard removed — the verification pill is now shown
// inside the avatar section of ProfileIdentityCard.

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
    themeMode: AppThemeMode,
    onToggleNotifications: (Boolean) -> Unit,
    onSetThemeMode: (AppThemeMode) -> Unit,
) {
    AppCard(elevated = true) {
        Column(verticalArrangement = Arrangement.spacedBy(AppDesignTokens.spacing.sm)) {
            Text("Preferences", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            ToggleRow(icon = Icons.Filled.Notifications, title = "Notifications", subtitle = "Task and event reminders", checked = notificationsEnabled, onToggle = onToggleNotifications)
            ThemeModeRow(selected = themeMode, onSelect = onSetThemeMode)
        }
    }
}

@Composable
private fun ThemeModeRow(
    selected: AppThemeMode,
    onSelect: (AppThemeMode) -> Unit,
) {
    val options = listOf(
        Triple(AppThemeMode.LIGHT, Icons.Filled.WbSunny, "Light"),
        Triple(AppThemeMode.SYSTEM, Icons.Filled.SettingsBrightness, "Auto"),
        Triple(AppThemeMode.DARK, Icons.Filled.DarkMode, "Dark"),
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppDesignTokens.spacing.sm),
    ) {
        Icon(Icons.Filled.DarkMode, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Column(modifier = Modifier.weight(1f)) {
            Text("Theme", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            Text("Light, Dark, or follow system", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            options.forEach { (mode, icon, label) ->
                val isSelected = selected == mode
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                            else MaterialTheme.colorScheme.surfaceVariant,
                        )
                        .clickable { onSelect(mode) }
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
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
