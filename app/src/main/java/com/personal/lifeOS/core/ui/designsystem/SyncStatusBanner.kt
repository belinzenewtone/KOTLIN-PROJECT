package com.personal.lifeOS.core.ui.designsystem

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Represents the current sync status of the app.
 *
 * Used to show status banners on screens.
 */
enum class SyncStatus {
    /** No sync in progress, all data is current */
    IDLE,

    /** Sync is in progress */
    SYNCING,

    /** Last sync failed - data may be stale */
    SYNC_FAILED,

    /** Device is offline - no sync possible */
    OFFLINE,

    /** Sync completed successfully */
    SYNC_COMPLETE,
}

/**
 * SyncStatusBanner - Shows sync status at the top of screens.
 *
 * Automatically displays appropriate message based on sync status.
 * Dismissable for SYNC_COMPLETE.
 *
 * Usage:
 * ```
 * var syncStatus by remember { mutableStateOf(SyncStatus.IDLE) }
 * var showSyncBanner by remember { mutableStateOf(true) }
 *
 * PageScaffold(
 *     title = "Finance",
 *     topBanner = {
 *         if (showSyncBanner) {
 *             SyncStatusBanner(
 *                 status = syncStatus,
 *                 onDismiss = { showSyncBanner = false }
 *             )
 *         }
 *     }
 * ) { ... }
 * ```
 */
@Composable
fun SyncStatusBanner(
    status: SyncStatus,
    modifier: Modifier = Modifier,
    onDismiss: (() -> Unit)? = null,
    onRetry: (() -> Unit)? = null,
) {
    when (status) {
        SyncStatus.IDLE,
        SyncStatus.SYNC_COMPLETE,
        -> {
            // No banner for idle or completed state
            // But show completed briefly if onDismiss is provided
            if (status == SyncStatus.SYNC_COMPLETE && onDismiss != null) {
                TopBanner(
                    message = "All changes synced",
                    tone = TopBannerTone.SUCCESS,
                    onDismiss = onDismiss,
                    modifier = modifier,
                )
            }
        }

        SyncStatus.SYNCING -> {
            TopBanner(
                message = "Syncing your data...",
                tone = TopBannerTone.INFO,
                modifier = modifier,
            )
        }

        SyncStatus.SYNC_FAILED -> {
            TopBanner(
                message = "Sync failed — your changes weren't saved",
                title = "Sync Error",
                tone = TopBannerTone.ERROR,
                actionLabel = if (onRetry != null) "Retry" else null,
                onAction = onRetry,
                onDismiss = onDismiss,
                modifier = modifier,
            )
        }

        SyncStatus.OFFLINE -> {
            TopBanner(
                message = "You're offline — changes will sync when reconnected",
                title = "Offline",
                tone = TopBannerTone.WARNING,
                onDismiss = onDismiss,
                modifier = modifier,
            )
        }
    }
}

/**
 * ImportStatusBanner - Shows import status at the top of screens.
 *
 * Used on Finance screen to show SMS import progress.
 */
enum class ImportStatus {
    /** No import in progress */
    IDLE,

    /** Import in progress */
    IMPORTING,

    /** Import completed successfully */
    IMPORT_COMPLETE,

    /** Import failed */
    IMPORT_FAILED,
}

@Composable
fun ImportStatusBanner(
    status: ImportStatus,
    modifier: Modifier = Modifier,
    message: String? = null,
    onDismiss: (() -> Unit)? = null,
    onRetry: (() -> Unit)? = null,
) {
    when (status) {
        ImportStatus.IDLE -> {
            // No banner
        }

        ImportStatus.IMPORTING -> {
            TopBanner(
                message = message ?: "Importing M-Pesa messages...",
                tone = TopBannerTone.INFO,
                modifier = modifier,
            )
        }

        ImportStatus.IMPORT_COMPLETE -> {
            TopBanner(
                message = message ?: "M-Pesa messages imported successfully",
                tone = TopBannerTone.SUCCESS,
                onDismiss = onDismiss,
                modifier = modifier,
            )
        }

        ImportStatus.IMPORT_FAILED -> {
            TopBanner(
                message = message ?: "M-Pesa import failed — please try again",
                title = "Import Error",
                tone = TopBannerTone.ERROR,
                actionLabel = if (onRetry != null) "Retry" else null,
                onAction = onRetry,
                onDismiss = onDismiss,
                modifier = modifier,
            )
        }
    }
}

/**
 * PermissionBanner - Shows permission request at the top of screens.
 *
 * Used when app needs runtime permissions.
 */
@Composable
fun PermissionBanner(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    grantLabel: String = "Allow",
    onGrant: (() -> Unit)? = null,
    onDismiss: (() -> Unit)? = null,
) {
    TopBanner(
        message = message,
        title = title,
        tone = TopBannerTone.INFO,
        actionLabel = grantLabel,
        onAction = onGrant,
        onDismiss = onDismiss,
        modifier = modifier,
    )
}
