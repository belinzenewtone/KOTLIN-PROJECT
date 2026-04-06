@file:Suppress("MaxLineLength")

package com.personal.lifeOS.core.permissions

import android.Manifest
import android.os.Build
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Message
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.accompanist.permissions.rememberPermissionState
import com.personal.lifeOS.navigation.AppRoute

/**
 * Contextual, non-nagging permission request orchestrator.
 *
 * Rules:
 *  • Each permission group is explained with a polished in-app rationale card
 *    **before** the system dialog fires — users know why before they decide.
 *  • Each rationale is shown at most **once**. "Not now" is a permanent dismiss.
 *  • Rationale cards appear at a natural moment, not on first app open:
 *      - Notifications → when the user first lands on the Home/Dashboard screen.
 *      - SMS (M-Pesa)  → when the user first opens the Finance screen.
 *  • If the system permission is already granted we skip everything silently.
 *
 * Place this composable inside the main [LifeOSNavHost] Box, below the
 * [NavHost] so the cards render on top of all content.
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun AppPermissionsOrchestrator(
    currentRoute: String?,
    viewModel: AppPermissionsViewModel = hiltViewModel(),
) {
    val notificationAsked by viewModel.notificationPermissionAsked.collectAsState()
    val smsAsked by viewModel.smsPermissionAsked.collectAsState()

    // ── Notification permission (Android 13 / TIRAMISU and above only) ──────
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val notificationPermState = rememberPermissionState(
            permission = Manifest.permission.POST_NOTIFICATIONS,
        )

        // Show rationale card only on Home, only once, only if not yet granted.
        var showNotificationRationale by rememberSaveable { mutableStateOf(false) }

        val isOnHome = currentRoute == AppRoute.Home
        LaunchedEffect(isOnHome, notificationAsked, notificationPermState.status.isGranted) {
            if (isOnHome && !notificationAsked && !notificationPermState.status.isGranted) {
                showNotificationRationale = true
            }
        }

        PermissionRationaleCard(
            visible = showNotificationRationale,
            icon = Icons.Outlined.Notifications,
            title = "Stay on top of reminders",
            description = "Allow notifications so LifeOS can remind you about tasks, " +
                "calendar events, and payment deadlines — right when you need them.",
            allowLabel = "Allow",
            denyLabel = "Not now",
            onAllow = {
                showNotificationRationale = false
                viewModel.markNotificationPermissionAsked()
                notificationPermState.launchPermissionRequest()
            },
            onDeny = {
                showNotificationRationale = false
                viewModel.markNotificationPermissionAsked()
            },
        )
    }

    // ── SMS / M-Pesa permission ──────────────────────────────────────────────
    val smsPermState = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.READ_SMS,
            Manifest.permission.RECEIVE_SMS,
        ),
    )

    val allSmsGranted = smsPermState.permissions.all { it.status.isGranted }

    var showSmsRationale by rememberSaveable { mutableStateOf(false) }

    val isOnFinance = currentRoute == AppRoute.Finance
    LaunchedEffect(isOnFinance, smsAsked, allSmsGranted) {
        if (isOnFinance && !smsAsked && !allSmsGranted) {
            showSmsRationale = true
        }
    }

    PermissionRationaleCard(
        visible = showSmsRationale,
        icon = Icons.Outlined.Message,
        title = "Auto-detect M-Pesa transactions",
        description = "Grant SMS access and LifeOS will instantly log your M-Pesa " +
            "payments and receipts — no manual entry needed.",
        allowLabel = "Allow",
        denyLabel = "No thanks",
        onAllow = {
            showSmsRationale = false
            viewModel.markSmsPermissionAsked()
            smsPermState.launchMultiplePermissionRequest()
        },
        onDeny = {
            showSmsRationale = false
            viewModel.markSmsPermissionAsked()
        },
    )
}
