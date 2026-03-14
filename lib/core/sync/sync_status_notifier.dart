import 'dart:async';

import 'package:beltech/core/sync/background_sync_coordinator.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:shared_preferences/shared_preferences.dart';

// ── Sync status model ─────────────────────────────────────────────────────────

enum SyncPhase { idle, syncing, done, failed }

class SyncStatus {
  const SyncStatus({
    required this.phase,
    this.lastSyncedAt,
  });

  final SyncPhase phase;

  /// Wall-clock time of the most recent successful sync (this session or from
  /// a previous session if SharedPreferences had a value on startup).
  final DateTime? lastSyncedAt;

  SyncStatus copyWith({SyncPhase? phase, DateTime? lastSyncedAt}) {
    return SyncStatus(
      phase: phase ?? this.phase,
      lastSyncedAt: lastSyncedAt ?? this.lastSyncedAt,
    );
  }
}

// ── Notifier ──────────────────────────────────────────────────────────────────

/// Tracks foreground sync status so the UI can react to sync lifecycle events.
///
/// On creation it reads the persisted last-sync timestamp so the home screen
/// can immediately show "Last synced X minutes ago".  Call [runSync] to
/// trigger an on-demand sync; the notifier transitions through
/// [SyncPhase.syncing] → [SyncPhase.done] / [SyncPhase.failed] and
/// auto-resets to [SyncPhase.idle] after [_resetDelay].
class SyncStatusNotifier extends StateNotifier<SyncStatus> {
  SyncStatusNotifier(this._coordinator)
      : super(const SyncStatus(phase: SyncPhase.idle)) {
    _loadPersistedLastSync();
  }

  final BackgroundSyncCoordinator _coordinator;

  // SharedPreferences key prefix written by SmsAutoImportService.
  static const String _smsKeyPrefix = 'mpesa_auto_sync_last_ms';

  // How long "Synced" stays visible before auto-reset to idle.
  static const Duration _resetDelay = Duration(seconds: 4);

  Timer? _resetTimer;

  /// Reads any stored last-sync timestamp and populates [state.lastSyncedAt]
  /// so the banner can show relative time even before the first foreground sync.
  Future<void> _loadPersistedLastSync() async {
    try {
      final prefs = await SharedPreferences.getInstance();
      final keys =
          prefs.getKeys().where((k) => k.startsWith(_smsKeyPrefix)).toList();
      DateTime? latest;
      for (final key in keys) {
        final ms = prefs.getInt(key);
        if (ms != null) {
          final dt = DateTime.fromMillisecondsSinceEpoch(ms);
          if (latest == null || dt.isAfter(latest)) latest = dt;
        }
      }
      if (latest != null && mounted) {
        state = SyncStatus(phase: SyncPhase.idle, lastSyncedAt: latest);
      }
    } catch (_) {
      // Non-critical; silently ignore.
    }
  }

  /// Triggers an on-demand sync through the coordinator and tracks progress.
  ///
  /// Safe to call concurrently: re-entrant calls are silently dropped while a
  /// sync is already [SyncPhase.syncing].
  Future<void> runSync() async {
    if (state.phase == SyncPhase.syncing) return;
    _resetTimer?.cancel();

    state = SyncStatus(
      phase: SyncPhase.syncing,
      lastSyncedAt: state.lastSyncedAt,
    );

    try {
      await _coordinator.syncNow();
      if (!mounted) return;
      final now = DateTime.now();
      state = SyncStatus(phase: SyncPhase.done, lastSyncedAt: now);
      _resetTimer = Timer(_resetDelay, () {
        if (mounted) {
          state = SyncStatus(
            phase: SyncPhase.idle,
            lastSyncedAt: state.lastSyncedAt,
          );
        }
      });
    } catch (_) {
      if (!mounted) return;
      state = SyncStatus(
        phase: SyncPhase.failed,
        lastSyncedAt: state.lastSyncedAt,
      );
      // Auto-clear failure badge after a short delay.
      _resetTimer = Timer(_resetDelay, () {
        if (mounted) {
          state = SyncStatus(
            phase: SyncPhase.idle,
            lastSyncedAt: state.lastSyncedAt,
          );
        }
      });
    }
  }

  @override
  void dispose() {
    _resetTimer?.cancel();
    super.dispose();
  }
}
