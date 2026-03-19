package com.personal.lifeOS.features.profile.presentation

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.personal.lifeOS.features.auth.presentation.AuthViewModel
import com.personal.lifeOS.ui.components.StyledSnackbarHost
import com.personal.lifeOS.ui.theme.AppSpacing
import com.personal.lifeOS.ui.theme.BackgroundDark
import com.personal.lifeOS.ui.theme.TextSecondary

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel = hiltViewModel(),
    authViewModel: AuthViewModel? = null,
    onSignOut: (() -> Unit)? = null,
) {
    val state by viewModel.uiState.collectAsState()
    val authState = authViewModel?.uiState?.collectAsState()?.value
    val snackbarHostState = remember { SnackbarHostState() }

    // Image picker launcher
    val imagePickerLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent(),
        ) { uri: Uri? ->
            uri?.let { viewModel.updateProfilePic(it) }
        }

    LaunchedEffect(state.successMessage) {
        state.successMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSuccessMessage()
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(BackgroundDark).statusBarsPadding()) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
                    .padding(horizontal = AppSpacing.ScreenHorizontal)
                    .padding(top = AppSpacing.ScreenTop, bottom = AppSpacing.BottomSafeWithFab),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Header
            Text(
                "Profile",
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(24.dp))

            ProfileAvatar(
                profilePicPath = state.profile.profilePicUri,
                avatarInitials = state.profile.avatarInitials,
                onChangePhoto = { imagePickerLauncher.launch("image/*") },
            )

            Spacer(Modifier.height(12.dp))

            Text(
                state.profile.name.ifEmpty { "Set up your profile" },
                style = MaterialTheme.typography.headlineMedium,
            )
            if (state.profile.email.isNotEmpty()) {
                Text(
                    state.profile.email,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                )
            }

            Spacer(Modifier.height(16.dp))

            // Member Since Banner
            MemberSinceBanner(memberSince = state.profile.memberSince)

            // Email Verification Banner
            EmailVerificationCard(
                authState = authState,
                onResendVerification = { authViewModel?.resendVerification() },
            )

            Spacer(Modifier.height(16.dp))

            // Edit / View Profile
            if (state.isEditing) {
                EditProfileCard(state, viewModel)
            } else {
                ProfileInfoCard(state, viewModel)
            }

            Spacer(Modifier.height(16.dp))

            SecurityCard(
                biometricEnabled = state.profile.isBiometricEnabled,
                onChangePassword = { viewModel.showPasswordDialog() },
                onToggleBiometric = { viewModel.toggleBiometric(it) },
            )

            Spacer(Modifier.height(16.dp))

            PreferencesCard(
                notificationsEnabled = state.profile.notificationsEnabled,
                onToggleNotifications = { viewModel.toggleNotifications(it) },
            )

            Spacer(Modifier.height(16.dp))

            CloudSyncCard(
                onBackup = { viewModel.syncToCloud() },
                onRestore = { viewModel.syncFromCloud() },
            )

            Spacer(Modifier.height(16.dp))

            AppInfoCard()

            // Sign Out
            if (onSignOut != null) {
                Spacer(Modifier.height(16.dp))
                SignOutButton(onClick = onSignOut)
            }
        }

        // Snackbar
        StyledSnackbarHost(
            hostState = snackbarHostState,
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = AppSpacing.BottomSafe),
        )
    }

    // Dialogs
    if (state.showPasswordDialog) {
        ChangePasswordDialog(state, viewModel)
    }
}
