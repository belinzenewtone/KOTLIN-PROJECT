package com.personal.lifeOS.features.profile.presentation

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Verified
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.personal.lifeOS.core.utils.DateUtils
import com.personal.lifeOS.features.auth.presentation.AuthUiState
import com.personal.lifeOS.ui.components.AccentGlassCard
import com.personal.lifeOS.ui.components.GlassCard
import com.personal.lifeOS.ui.theme.Accent
import com.personal.lifeOS.ui.theme.BackgroundDark
import com.personal.lifeOS.ui.theme.Error
import com.personal.lifeOS.ui.theme.GlassBorder
import com.personal.lifeOS.ui.theme.GlassWhite
import com.personal.lifeOS.ui.theme.Info
import com.personal.lifeOS.ui.theme.Primary
import com.personal.lifeOS.ui.theme.Success
import com.personal.lifeOS.ui.theme.SurfaceDark
import com.personal.lifeOS.ui.theme.TextPrimary
import com.personal.lifeOS.ui.theme.TextSecondary
import com.personal.lifeOS.ui.theme.TextTertiary
import com.personal.lifeOS.ui.theme.Warning
import java.io.File

@Composable
internal fun ProfileAvatar(
    profilePicPath: String,
    avatarInitials: String,
    onChangePhoto: () -> Unit,
) {
    val hasPhoto = profilePicPath.isNotEmpty() && File(profilePicPath).exists()

    Box(contentAlignment = Alignment.BottomEnd) {
        Box(
            modifier =
                Modifier
                    .size(110.dp)
                    .clip(CircleShape)
                    .then(
                        if (!hasPhoto) {
                            Modifier.background(
                                brush =
                                    Brush.linearGradient(
                                        colors = listOf(Primary, Accent),
                                    ),
                            )
                        } else {
                            Modifier
                        },
                    )
                    .border(3.dp, Primary.copy(alpha = 0.4f), CircleShape)
                    .clickable(onClick = onChangePhoto),
            contentAlignment = Alignment.Center,
        ) {
            if (hasPhoto) {
                Image(
                    painter = rememberAsyncImagePainter(model = File(profilePicPath)),
                    contentDescription = "Profile photo",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Text(
                    text = avatarInitials,
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Bold,
                    color = BackgroundDark,
                )
            }
        }

        Box(
            modifier =
                Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Primary)
                    .border(2.dp, BackgroundDark, CircleShape)
                    .clickable(onClick = onChangePhoto),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.CameraAlt,
                contentDescription = "Change photo",
                tint = BackgroundDark,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Composable
internal fun MemberSinceBanner(memberSince: Long) {
    AccentGlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Box(
                modifier =
                    Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Primary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Star,
                    contentDescription = null,
                    tint = Accent,
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(Modifier.width(14.dp))
            Column {
                Text(
                    text = "Member Since",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                )
                Text(
                    text =
                        if (memberSince > 0L) {
                            DateUtils.formatDate(memberSince, "EEEE, MMMM dd, yyyy")
                        } else {
                            "Save your profile to start"
                        },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                )
            }
        }
    }
}

@Composable
internal fun EmailVerificationCard(
    authState: AuthUiState?,
    onResendVerification: () -> Unit,
) {
    if (authState == null) return

    Spacer(Modifier.height(12.dp))
    if (authState.emailVerified) {
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Verified,
                    contentDescription = null,
                    tint = Success,
                    modifier = Modifier.size(24.dp),
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Account Verified",
                        style = MaterialTheme.typography.titleMedium,
                        color = Success,
                    )
                    Text(
                        text = "Your email has been confirmed",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                    )
                }
            }
        }
    } else {
        AccentGlassCard(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onResendVerification),
            accentColor = Warning,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.MarkEmailUnread,
                    contentDescription = null,
                    tint = Warning,
                    modifier = Modifier.size(24.dp),
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Email Not Verified",
                        style = MaterialTheme.typography.titleMedium,
                        color = Warning,
                    )
                    Text(
                        text = "Tap to resend verification link",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                    )
                }
            }
        }
    }
}

@Composable
internal fun ProfileInfoCard(
    state: ProfileUiState,
    viewModel: ProfileViewModel,
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Personal Info", style = MaterialTheme.typography.titleMedium)
                IconButton(onClick = { viewModel.startEditing() }) {
                    Icon(Icons.Filled.Edit, "Edit", tint = Primary)
                }
            }

            Spacer(Modifier.height(8.dp))
            InfoRow(Icons.Filled.Person, "Name", state.profile.name.ifEmpty { "Not set" })
            Spacer(Modifier.height(14.dp))
            InfoRow(Icons.Outlined.Email, "Email", state.profile.email.ifEmpty { "Not set" })
            Spacer(Modifier.height(14.dp))
            InfoRow(Icons.Outlined.Phone, "Phone", state.profile.phone.ifEmpty { "Not set" })
        }
    }
}

@Composable
private fun InfoRow(
    icon: ImageVector,
    label: String,
    value: String,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier =
                Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Primary.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center,
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
internal fun EditProfileCard(
    state: ProfileUiState,
    viewModel: ProfileViewModel,
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("Edit Profile", style = MaterialTheme.typography.titleMedium)

            GlassTextField("Full Name", state.editName) { viewModel.updateEditName(it) }
            GlassTextField("Email", state.editEmail) { viewModel.updateEditEmail(it) }
            GlassTextField("Phone", state.editPhone) { viewModel.updateEditPhone(it) }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(
                    onClick = { viewModel.cancelEditing() },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                ) {
                    Text("Cancel")
                }
                Button(
                    onClick = { viewModel.saveProfile() },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = Primary,
                            contentColor = BackgroundDark,
                        ),
                ) {
                    Text("Save", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun GlassTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
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

@Composable
internal fun SecurityCard(
    biometricEnabled: Boolean,
    onChangePassword: () -> Unit,
    onToggleBiometric: (Boolean) -> Unit,
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Security", style = MaterialTheme.typography.titleMedium)

            SettingsRow(
                icon = Icons.Filled.Lock,
                iconColor = Warning,
                title = "Change Password",
                subtitle = "Update your app password",
                onClick = onChangePassword,
            )

            SettingsToggleRow(
                icon = Icons.Filled.Fingerprint,
                iconColor = Primary,
                title = "Biometric Lock",
                subtitle = "Use fingerprint to unlock",
                checked = biometricEnabled,
                onToggle = onToggleBiometric,
            )
        }
    }
}

@Composable
internal fun PreferencesCard(
    notificationsEnabled: Boolean,
    onToggleNotifications: (Boolean) -> Unit,
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Preferences", style = MaterialTheme.typography.titleMedium)
            SettingsToggleRow(
                icon = Icons.Filled.Notifications,
                iconColor = Accent,
                title = "Notifications",
                subtitle = "Event and task reminders",
                checked = notificationsEnabled,
                onToggle = onToggleNotifications,
            )
        }
    }
}

@Composable
internal fun CloudSyncCard(
    onBackup: () -> Unit,
    onRestore: () -> Unit,
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Cloud Sync", style = MaterialTheme.typography.titleMedium)

            SettingsRow(
                icon = Icons.Filled.CloudUpload,
                iconColor = Info,
                title = "Backup to Cloud",
                subtitle = "Push data to Supabase",
                onClick = onBackup,
            )

            SettingsRow(
                icon = Icons.Filled.CloudDownload,
                iconColor = Accent,
                title = "Restore from Cloud",
                subtitle = "Pull data from Supabase",
                onClick = onRestore,
            )
        }
    }
}

@Composable
internal fun AppInfoCard() {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Info, null, tint = TextTertiary, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
            Column {
                Text("BELTECH", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                Text(
                    "Version 1.0.0 • Innovate and Create",
                    style = MaterialTheme.typography.labelSmall,
                )
            }
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
        shape = RoundedCornerShape(16.dp),
        colors =
            ButtonDefaults.buttonColors(
                containerColor = Error.copy(alpha = 0.15f),
                contentColor = Error,
            ),
    ) {
        Icon(Icons.AutoMirrored.Filled.Logout, null, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text("Sign Out", fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(GlassWhite)
                .clickable(onClick = onClick)
                .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(iconColor.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = iconColor, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = TextTertiary)
        }
        Icon(Icons.Outlined.ChevronRight, null, tint = TextTertiary, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun SettingsToggleRow(
    icon: ImageVector,
    iconColor: Color,
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
                .background(GlassWhite)
                .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(iconColor.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
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
            colors =
                SwitchDefaults.colors(
                    checkedThumbColor = BackgroundDark,
                    checkedTrackColor = Primary,
                    uncheckedThumbColor = TextTertiary,
                    uncheckedTrackColor = GlassWhite,
                ),
        )
    }
}

@Composable
internal fun ChangePasswordDialog(
    state: ProfileUiState,
    viewModel: ProfileViewModel,
) {
    AlertDialog(
        onDismissRequest = { viewModel.hidePasswordDialog() },
        containerColor = SurfaceDark,
        shape = RoundedCornerShape(24.dp),
        title = { Text("Change Password", color = TextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                PasswordField("Current Password", state.currentPassword) {
                    viewModel.updateCurrentPassword(it)
                }
                PasswordField("New Password", state.newPassword) {
                    viewModel.updateNewPassword(it)
                }
                PasswordField("Confirm Password", state.confirmPassword) {
                    viewModel.updateConfirmPassword(it)
                }
                if (state.passwordError != null) {
                    Text(
                        state.passwordError,
                        color = Error,
                        style = MaterialTheme.typography.labelSmall,
                    )
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
        visualTransformation = PasswordVisualTransformation(),
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors =
            OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Primary,
                unfocusedBorderColor = GlassBorder,
                focusedContainerColor = GlassWhite,
                unfocusedContainerColor = GlassWhite,
                cursorColor = Primary,
            ),
    )
}
