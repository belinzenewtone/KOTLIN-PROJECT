package com.personal.lifeOS.features.profile.presentation

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.personal.lifeOS.core.ui.designsystem.PageScaffold
import com.personal.lifeOS.features.auth.presentation.AuthViewModel
import com.personal.lifeOS.ui.components.StyledSnackbarHost
import com.personal.lifeOS.ui.theme.AppSpacing

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel = hiltViewModel(),
    authViewModel: AuthViewModel? = null,
    onSignOut: (() -> Unit)? = null,
) {
    val state by viewModel.uiState.collectAsState()
    val authState = authViewModel?.uiState?.collectAsState()?.value
    val snackbarHostState = remember { SnackbarHostState() }

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

    Box(modifier = Modifier.fillMaxSize()) {
        PageScaffold(
            title = "Profile",
            subtitle = "Personal details, security, and sync controls.",
            contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = AppSpacing.BottomSafeWithFab),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ProfileIdentityCard(
                    profile = state.profile,
                    authState = authState,
                    onChangePhoto = { imagePickerLauncher.launch("image/*") },
                )

                if (state.isEditing) {
                    ProfileEditorCard(state = state, viewModel = viewModel)
                } else {
                    ProfileDetailsCard(state = state, onEdit = viewModel::startEditing)
                }

                ProfileSecurityCard(
                    biometricEnabled = state.profile.isBiometricEnabled,
                    onChangePassword = viewModel::showPasswordDialog,
                    onToggleBiometric = viewModel::toggleBiometric,
                )

                ProfilePreferencesCard(
                    notificationsEnabled = state.profile.notificationsEnabled,
                    themeMode = state.themeMode,
                    onToggleNotifications = viewModel::toggleNotifications,
                    onSetThemeMode = viewModel::setThemeMode,
                )

                ProfileCloudSyncCard(
                    onBackup = viewModel::syncToCloud,
                    onRestore = viewModel::syncFromCloud,
                )

                ProfileAppInfoCard()
                if (onSignOut != null) {
                    SignOutButton(onClick = onSignOut)
                }
                Spacer(modifier = Modifier.height(4.dp))
            }
        }

        StyledSnackbarHost(
            hostState = snackbarHostState,
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(horizontal = AppSpacing.ScreenHorizontal),
        )
    }

    if (state.showPasswordDialog) {
        ChangePasswordDialog(
            state = state,
            viewModel = viewModel,
        )
    }
}
